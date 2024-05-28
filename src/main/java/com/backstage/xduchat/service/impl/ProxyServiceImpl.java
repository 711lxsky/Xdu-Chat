package com.backstage.xduchat.service.impl;

import com.backstage.xduchat.Exception.HttpException;
import com.backstage.xduchat.Utils.BuildResponseJsonObject;
import com.backstage.xduchat.Utils.JsonUtil;
import com.backstage.xduchat.config.DataConfig;
import com.backstage.xduchat.config.ProxyConfig;
import com.backstage.xduchat.domain.Result;
import com.backstage.xduchat.domain.entity.DialogueTime;
import com.backstage.xduchat.service.DialogueTimeService;
import com.backstage.xduchat.service.ProxyService;
import com.backstage.xduchat.setting_enum.DialogueTimeConstant;
import com.backstage.xduchat.setting_enum.ExceptionConstant;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.Resource;
import java.io.IOException;
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

    private final DialogueTimeService dialogueTimeService;

    private final RequestForXDUCHAT requestForXDUCHAT;

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

    public ProxyServiceImpl(JsonUtil jsonUtil, DataConfig dataConfig, ProxyConfig proxyConfig,
                            DialogueTimeService dialogueTimeService, RequestForXDUCHAT requestForXDUCHAT){
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
        this.dialogueTimeService = dialogueTimeService;
        this.requestForXDUCHAT = requestForXDUCHAT;
    }

    public Object proxy(String jsonParam) throws HttpException {
        // 逐级校验数据
        // 最外层参数
        JsonNode jsonParameters = jsonUtil.getJsonNode(jsonParam);
        if(this.judgeJsonDataIsNull(jsonParameters)){
            return responseJsonFail(ExceptionConstant.DataError.getMessage_ZH());
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
            return responseJsonFail(ExceptionConstant.ParameterError.getMessage_ZH());
        }
        // uid
        JsonNode jsonUserId = jsonParameters.get(dataConfig.getParameterUserid());
        if(this.judgeJsonDataIsNull(jsonUserId)){
            if(needStream){
                return responseSSE(ExceptionConstant.UserIdIsNull.getMessage_ZH());
            }
            return responseJsonFail(ExceptionConstant.UserIdIsNull.getMessage_ZH());
        }
        String userId = jsonUserId.asText();
        // record_id
        JsonNode jsonRecordId = jsonParameters.get(dataConfig.getParameterRecordId());
        if(this.judgeJsonDataIsNull(jsonRecordId)){
            if(needStream){
                return responseSSE(ExceptionConstant.RecordIdIsNull.getMessage_ZH());
            }
            return responseJsonFail(ExceptionConstant.RecordIdIsNull.getMessage_ZH());
        }
        String recordId = jsonRecordId.asText();
        // 这里先根据 userId 和 recordId 去次数表里面做一个查询， 搞一个限制
        int curDialogueTime = curDialogueTime(userId, recordId);
        if(curDialogueTime >= proxyConfig.getDialogueTimeMax()){
            if(needStream) {
                return responseSSE(ExceptionConstant.DialogueTimeUpToLimit.getMessage_ZH());
            }
            return responseJsonFail(ExceptionConstant.DialogueTimeUpToLimit.getMessage_ZH());
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
                HttpStatus refreshMark = HttpStatus.valueOf(proxyConfig.getRepeatRequestResponseStatus());
                return new ResponseEntity<>(refreshMark);
            }
            try {
                String responseFromXDUCHAT = this.requestForXDUCHAT.requestForXDUCHAT(userId, recordId, jsonMessages, curDialogueTime);
                String dialogueTimeInfo =  dialogueTimeService.getInformationForDialogueTime(curDialogueTime);
                responseFromXDUCHAT += dialogueTimeInfo;
//                responseFromXDUCHAT = responseFromXDUCHAT.replaceAll("\\n", "\\\\n");
                if(needStream){
                    JsonNode [] responseInfo = this.buildSSEFormatResponse(responseFromXDUCHAT);
                    return this.responseSSEFromXDUCHAT(responseInfo);
                }
                // 这里返回的非SSE形式的JSON,但是不是自定义的Result
                return this.normalNotSSEResponse(responseFromXDUCHAT);
            }
            catch (HttpException e){
                return responseJsonFail(e.getMessage());
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

    private JsonNode normalNotSSEResponse(String baseInfo){
        return buildResponseJsonObject.buildResponseForSSE(dataConfig.getResponseJsonFormatFirst(), baseInfo);
//        baseInfo = baseInfo.replaceAll("\\n", "\\\\n");
//        return dataConfig.getNormalNotSSEResponseConnectStr1()
//                + baseInfo
//                + dataConfig.getNormalNotSSEResponseConnectStr2();
    }

    private JsonNode[] buildSSEFormatResponse(String xduchatResponse){
        String [] splitRes  = xduchatResponse.split("");
//        log.info("strings: {}", Arrays.toString(splitRes));
        int size = splitRes.length;
        JsonNode [] responseInfo = new JsonNode [size + 1];
        for(int i = 0; i <= size; i ++){
            JsonNode info = buildSSEResponseWithJsonFormat(splitRes, i, size);
            responseInfo[i] = info;
        }
        return responseInfo;
    }

    @Resource
    private BuildResponseJsonObject buildResponseJsonObject;


    private JsonNode buildSSEResponseWithJsonFormat(String[] contents, int index, int size){
        if(index == 0){
            return buildResponseJsonObject.buildResponseForSSE(dataConfig.getResponseJsonFormatFirst(), contents[index]);
        }
        else if(index == size) {
            return buildResponseJsonObject.buildResponseForSSE(dataConfig.getResponseJsonFormatCommon(), "");
        }
        else {
            return buildResponseJsonObject.buildResponseForSSE(dataConfig.getResponseJsonFormatCommon(), contents[index]);
        }
    }

    private Result<?> responseJsonFail(String responseMessage){
        return Result.fail(responseMessage);
    }

    private SseEmitter responseSSEFromXDUCHAT(JsonNode[] infos){
        SseEmitter sseEmitter = new SseEmitter(Long.MAX_VALUE);
        executorService.execute(() -> {
            try {
                for(JsonNode info : infos){
                    log.info("info {}", info);
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
                JsonNode info = buildResponseJsonObject.buildResponseForSSE(dataConfig.getResponseJsonFormatFirst(), baseInfo);
                sseEmitter.send(info);
            } catch (IOException e) {
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
