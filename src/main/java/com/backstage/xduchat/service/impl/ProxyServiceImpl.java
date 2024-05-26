package com.backstage.xduchat.service.impl;

import cn.hutool.core.util.StrUtil;
import com.backstage.xduchat.Exception.HttpException;
import com.backstage.xduchat.Utils.JsonUtil;
import com.backstage.xduchat.config.DataConfig;
import com.backstage.xduchat.config.ProxyConfig;
import com.backstage.xduchat.domain.Result;
import com.backstage.xduchat.domain.dto.MessageOPENAI;
import com.backstage.xduchat.domain.dto.MessageXDUCHAT;
import com.backstage.xduchat.domain.dto.ParametersXDUCHAT;
import com.backstage.xduchat.domain.entity.DialogueTime;
import com.backstage.xduchat.domain.entity.GeneralRecord;
import com.backstage.xduchat.service.DialogueTimeService;
import com.backstage.xduchat.service.GeneralRecordService;
import com.backstage.xduchat.service.ProxyService;
import com.backstage.xduchat.setting_enum.DialogueTimeConstant;
import com.backstage.xduchat.setting_enum.ExceptionConstant;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @Author: 711lxsky
 * @Description:
 */

@Service
@Log4j2
public class ProxyServiceImpl implements ProxyService {

    private final JsonUtil jsonUtil;

    private final DataConfig dataConfig;

    private final ProxyConfig proxyConfig;

    private final ExecutorService executorService;

    private final RestTemplate restTemplate;

    private final GeneralRecordService generalRecordService;

    private final DialogueTimeService dialogueTimeService;

    private final ConcurrentHashMap<String, ReentrantLock> requestLocks = new ConcurrentHashMap<>();

    // 核心线程数等于CPU核心数，以充分利用CPU资源
    private final int corePoolSize = Runtime.getRuntime().availableProcessors();

    // 最大线程数不超过2倍核心数，以防过多的上下文切换
    private final int maximumPoolSize = corePoolSize * 2;

    // 空闲线程存活时间，例如60秒
    private final long keepAliveTime = 60L;

    // 时间单位为秒
    private final TimeUnit unit = TimeUnit.SECONDS;

    // 使用有界队列，容量为最大线程数的1.5倍，以平衡线程池和队列之间的任务处理
    private final int queueCapacity = (int) (maximumPoolSize * 1.5);

    // 使用LinkedBlockingQueue，它具有无限容量，但在实践中表现得像一个有界队列，因为它是基于链表实现的，插入操作很快
    private final BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>(queueCapacity);

    // 使用默认的线程工厂和拒绝策略
    private final RejectedExecutionHandler handler = new ThreadPoolExecutor.AbortPolicy();


    public ProxyServiceImpl(JsonUtil jsonUtil, DataConfig dataConfig, ProxyConfig proxyConfig, RestTemplate restTemplate,
                            GeneralRecordService generalRecordService, DialogueTimeService dialogueTimeService){
        this.jsonUtil = jsonUtil;
        this.dataConfig = dataConfig;
        this.proxyConfig = proxyConfig;
        this.executorService = new ThreadPoolExecutor(
                corePoolSize,
                maximumPoolSize,
                keepAliveTime,
                unit,
                workQueue,
                Executors.defaultThreadFactory(),
                handler
        );
        this.restTemplate = restTemplate;
        this.generalRecordService = generalRecordService;
        this.dialogueTimeService = dialogueTimeService;
    }

    public Object proxy(String jsonParam) throws HttpException {
        // 逐级校验数据
        // 最外层参数
        JsonNode jsonParameters = jsonUtil.getJsonNode(jsonParam);
        if(this.judgeJsonDataIsNull(jsonParameters)){
            return responseJson(ExceptionConstant.DataError.getMessage_ZH());
        }
        // Stream
        JsonNode jsonStream = jsonParameters.get(dataConfig.getParameterStream());
        boolean needStream;
        if(this.judgeJsonDataIsNull(jsonStream)){
            needStream = false;
        }
        else {
            needStream = jsonStream.asBoolean();
        }
        // messages
        JsonNode jsonMessages = jsonParameters.get(proxyConfig.getParameterMessages());
        if(this.judgeJsonDataIsNull(jsonMessages)){
            if(needStream){
                return responseSSE(ExceptionConstant.ParameterError.getMessage_ZH());
            }
            return responseJson(ExceptionConstant.ParameterError.getMessage_ZH());
        }
        // uid
        JsonNode jsonUserId = jsonParameters.get(dataConfig.getParameterUserid());
        if(this.judgeJsonDataIsNull(jsonUserId)){
            if(needStream){
                return responseSSE(ExceptionConstant.UserIdIsNull.getMessage_ZH());
            }
            return responseJson(ExceptionConstant.UserIdIsNull.getMessage_ZH());
        }
        String userId = jsonUserId.asText();
        // record_id
        JsonNode jsonRecordId = jsonParameters.get(dataConfig.getParameterRecordId());
        if(this.judgeJsonDataIsNull(jsonRecordId)){
            if(needStream){
                return responseSSE(ExceptionConstant.RecordIdIsNull.getMessage_ZH());
            }
            return responseJson(ExceptionConstant.RecordIdIsNull.getMessage_ZH());
        }
        String recordId = jsonRecordId.asText();
        // 这里先根据 userId 和 recordId 去次数表里面做一个查询， 搞一个限制
        int curDialogueTime = curDialogueTime(userId, recordId);
        if(curDialogueTime >= proxyConfig.getDialogueTimeMax()){
            if(needStream) {
                return responseSSE(ExceptionConstant.DialogueTimeUpToLimit.getMessage_ZH());
            }
            return responseJson(ExceptionConstant.DialogueTimeUpToLimit.getMessage_ZH());
        }
        String identifier = userId + recordId;
        ReentrantLock lock = requestLocks.computeIfAbsent(identifier, key -> new ReentrantLock());
        boolean waiting = false;
        try {
            if(lock.isLocked()){
                waiting = true;
            }
            lock.lock();
            if(waiting){
                // responseEntity返回的status为202
//                if(needStream){
//                    return responseSSE(proxyConfig.getRepeatRequest());
//                }
                HttpStatus refreshMark = HttpStatus.valueOf(proxyConfig.getRepeatRequestResponseStatus());
                return new ResponseEntity<>(refreshMark);
            }
            try {
                String responseFromXDUCHAT = this.requestForXDUCHAT(userId, recordId, jsonMessages, curDialogueTime);
                String dialogueTimeInfo =  dialogueTimeService.getInformationForDialogueTime(curDialogueTime);
                responseFromXDUCHAT += dialogueTimeInfo;
                if(needStream){
                    String [] responseInfo = this.buildSSEFormatResponse(responseFromXDUCHAT);
                    return this.responseSSEFromXDUCHAT(responseInfo);
                }
                // 这里返回的非SSE形式的JSON,但是不是自定义的Result
                return this.normalNotSSEResponse(responseFromXDUCHAT);
            }
            catch (HttpException e){
                return responseJson(e.getMessage());
            }
        }
        finally {
            lock.unlock();
        }
    }

    private int curDialogueTime(String uid, String dialogueId) {
            DialogueTime dialogueTime = this.dialogueTimeService.getByUidAndDialogueId(uid, dialogueId);
            if(Objects.isNull(dialogueTime)){
                try {
                    this.dialogueTimeService.insertOne(uid, dialogueId);
                }
                catch (Exception e){
                    return Integer.parseInt(DialogueTimeConstant.TIME_ERROR_FLAG.getFlag());
                }
                return Integer.parseInt(DialogueTimeConstant.DEFAULT_TIME.getFlag());
            }
            else if (Objects.equals(dialogueTime.getId(), Long.valueOf(DialogueTimeConstant.FLAG_ID_NULL.getFlag()))){
                return Integer.parseInt(DialogueTimeConstant.TIME_ERROR_FLAG.getFlag());
            }
            return dialogueTime.getTime();
    }

    private String normalNotSSEResponse(String baseInfo){
        return dataConfig.getNormalNotSSEResponseConnectStr1()
                + baseInfo
                + dataConfig.getNormalNotSSEResponseConnectStr2();
    }

    private String[] buildSSEFormatResponse(String xduchatResponse){
        String [] splitRes  = xduchatResponse.split("");
        log.info("strings: {}", Arrays.toString(splitRes));
        int size = splitRes.length;
        String [] responseInfo = new String [size + 1];
        for(int i = 0; i <= size; i ++){
            String info = getInfo(splitRes, i, size);
            responseInfo[i] = info;
        }
        return responseInfo;
    }

    private String requestForXDUCHAT(String userId, String recordId, JsonNode jsonMessages, int curDialogueTime) throws HttpException{
        if(jsonMessages.isArray()){
            try {
                List<MessageOPENAI> messagesOpenai = jsonUtil.getObjectMapper().convertValue(jsonMessages, new TypeReference<>() {});
                List<MessageXDUCHAT> messagesXduchat = this.convertMessage(messagesOpenai);
                // 重新构造参数
                List<String> params = new ArrayList<>();
                ParametersXDUCHAT parametersXDUCHAT = new ParametersXDUCHAT(messagesXduchat, params);
                log.info(parametersXDUCHAT.toString());
                JsonNode xduchatResponse = restTemplate.postForObject(proxyConfig.getXduchatApiUrl(), parametersXDUCHAT, JsonNode.class);
                log.info("general-response: {}", xduchatResponse);
                if(Objects.isNull(xduchatResponse)){
                   throw new HttpException(ExceptionConstant.ResponseNull.getMessage_ZH());
                }
                String realResponse = xduchatResponse.get("response").asText();
                log.info("real-response: {}", realResponse);
                if(! StringUtils.hasText(realResponse)){
                    throw new HttpException(ExceptionConstant.ResponseNull.getMessage_ZH());
                }
                MessageOPENAI responseMessage = new MessageOPENAI(proxyConfig.getParameterRoleAssistant(), realResponse);
                messagesOpenai.add(responseMessage);
                String jsonGeneralRecords = jsonUtil.toJson(messagesOpenai);
                GeneralRecord generalRecord = new GeneralRecord(userId, recordId, new Date(System.currentTimeMillis()), jsonGeneralRecords);
                // 持久化， 这里先判断一下数据库是否出现了问题， 没有才做持久化
                if(curDialogueTime != Integer.parseInt(DialogueTimeConstant.TIME_ERROR_FLAG.getFlag())){
                    try {
                        generalRecordService.save(generalRecord);
                        dialogueTimeService.addTime(userId, recordId, curDialogueTime);
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
                return realResponse;
            }
            catch (RestClientException e){
                log.info(e.getCause() + e.getMessage());
                e.printStackTrace();
                if(e.getCause() instanceof SocketTimeoutException){
                    throw new HttpException(ExceptionConstant.TimeOut.getMessage_ZH());
                }
                throw new HttpException(ExceptionConstant.InternalServerError.getMessage_ZH());
            }
        }
        throw new HttpException(ExceptionConstant.DataError.getMessage_ZH());
    }

    private String getInfo(String[] splitRes, int i, int size) {
        String string = "";
        String info;
        if(i == 0){
            string = splitRes[i];
            info =
                    proxyConfig.getConnectStrBas1()
                            + proxyConfig.getConnectStrFirst()
                            + "\""
                            + string
                            + "\""
                            + proxyConfig.getConnectStrBas2();
        }
        else if(i == size){
            info =
                    proxyConfig.getConnectStrBas1()
                            + proxyConfig.getConnectStrLast();

        }
        else {
            string = splitRes[i];
            info =
                    proxyConfig.getConnectStrBas1()
                            + proxyConfig.getConnectStrIndexMid()
                            + "\""
                            + string
                            + "\""
                            + proxyConfig.getConnectStrBas2();
        }
        return info;
    }

    private List<MessageXDUCHAT> convertMessage(List<MessageOPENAI> messagesOpenai) {
        List<MessageXDUCHAT> messagesXDUCHAT = new ArrayList<>();
        for(MessageOPENAI messageOpenai : messagesOpenai) {
            if(StrUtil.equals(messageOpenai.getRole(), proxyConfig.getParameterRoleSystem())){
                continue;
            }
            else if(StrUtil.equals(messageOpenai.getRole(), proxyConfig.getParameterRoleUser())){
                messagesXDUCHAT.add(new MessageXDUCHAT(proxyConfig.getParameterRoleHUMAN(), messageOpenai.getContent()));
            }
            else if(StrUtil.equals(messageOpenai.getRole(), proxyConfig.getParameterRoleAssistant())){
                messagesXDUCHAT.add(new MessageXDUCHAT(proxyConfig.getParameterRoleBOT(), messageOpenai.getContent()));
            }
        }
        return messagesXDUCHAT;
    }

    private Result<?> responseJson(String responseMessage){
        return Result.fail(responseMessage);
    }

    private SseEmitter responseSSEFromXDUCHAT(String[] infos){
        SseEmitter sseEmitter = new SseEmitter(Long.MAX_VALUE);
        executorService.execute(() -> {
            try {
                for(String info : infos){
                    sseEmitter.send(info);
                    Thread.sleep(proxyConfig.getSSESendTime());
                }
            } catch (IOException | InterruptedException e) {
                sseEmitter.completeWithError(e);
            }
            finally {
                sseEmitter.complete();
            }
        });
        return sseEmitter;
    }

    private SseEmitter responseSSE(String baseInfo){
        SseEmitter sseEmitter = new SseEmitter(Long.MAX_VALUE);
        executorService.execute(() -> {
            try {
                String fullResponseInfo =
                        proxyConfig.getConnectStrBas1()
                                + proxyConfig.getConnectStrFirst()
                                + "\""
                                + baseInfo
                                + "\""
                                + proxyConfig.getConnectStrBas2();
                String [] responseInfo = {fullResponseInfo};
                for(String info : responseInfo){
                    sseEmitter.send(info);
                    Thread.sleep(proxyConfig.getSSESendTime());
                }
            } catch (IOException | InterruptedException e) {
                sseEmitter.completeWithError(e);
            }
            finally {
                sseEmitter.complete();
            }
        });
        return sseEmitter;
    }

    private boolean judgeJsonDataIsNull(JsonNode jsonNode){
        if(Objects.isNull(jsonNode)){
            return true;
        }
        String jsonNodeText = jsonUtil.parseJsonNodeToString(jsonNode);
        return !StringUtils.hasText(jsonNodeText);
    }


}
