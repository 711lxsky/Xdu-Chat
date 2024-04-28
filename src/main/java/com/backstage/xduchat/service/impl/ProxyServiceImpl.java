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
import com.backstage.xduchat.domain.entity.GeneralRecord;
import com.backstage.xduchat.service.GeneralRecordService;
import com.backstage.xduchat.service.ProxyService;
import com.backstage.xduchat.setting_enum.ExceptionConstant;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
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

    private final ConcurrentHashMap<String, ReentrantLock> requestLocks = new ConcurrentHashMap<>();

    public ProxyServiceImpl(JsonUtil jsonUtil, DataConfig dataConfig, ProxyConfig proxyConfig, ExecutorService executorService, RestTemplate restTemplate, GeneralRecordService generalRecordService){
        this.jsonUtil = jsonUtil;
        this.dataConfig = dataConfig;
        this.proxyConfig = proxyConfig;
        this.executorService = executorService;
        this.restTemplate = restTemplate;
        this.generalRecordService = generalRecordService;
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
                String responseFromXDUCHAT = this.requestForXDUCHAT(userId, recordId, jsonMessages);
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

    private String normalNotSSEResponse(String baseInfo){
        return dataConfig.getNormalNotSSEResponseConnectStr1()
                + baseInfo
                + dataConfig.getNormalNotSSEResponseConnectStr2();
    }

    private String[] buildSSEFormatResponse(String xduchatResponse){
        String [] splitRes  = xduchatResponse.split("");
        log.info("strings: {}", Arrays.toString(splitRes));
        int size = splitRes.length;
        String [] responseInfo = new String [size];
        for(int i = 0; i < size; i ++){
            String info = getInfo(splitRes, i, size);
            responseInfo[i] = info;
        }
        return responseInfo;
    }

    private String requestForXDUCHAT(String userId, String recordId, JsonNode jsonMessages) throws HttpException{
        if(jsonMessages.isArray()){
            try {
                List<MessageOPENAI> messagesOpenai = jsonUtil.getObjectMapper().convertValue(jsonMessages, new TypeReference<>() {});
                List<MessageXDUCHAT> messagesXduchat = this.convertMessage(messagesOpenai);
                // 重新构造参数
                List<String> params = new ArrayList<>();
                ParametersXDUCHAT parametersXDUCHAT = new ParametersXDUCHAT(messagesXduchat, params);
                log.info(parametersXDUCHAT.toString());
                String xduchatResponse = restTemplate.postForObject(proxyConfig.getXduchatApiUrlNew(), parametersXDUCHAT, String.class);
                MessageOPENAI responseMessage = new MessageOPENAI(proxyConfig.getParameterRoleAssistant(), xduchatResponse);
                messagesOpenai.add(responseMessage);
                String jsonGeneralRecords = jsonUtil.toJson(messagesOpenai);
                GeneralRecord generalRecord = new GeneralRecord(userId, recordId, new Date(System.currentTimeMillis()), jsonGeneralRecords);
                // 持久化
                generalRecordService.save(generalRecord);
                if(! StringUtils.hasText(xduchatResponse)){
                    throw new HttpException(ExceptionConstant.ResponseNull.getMessage_ZH());
                }
                return xduchatResponse;
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
        String string = splitRes[i];
        String info;
        if(i == 0){
            info =
                    proxyConfig.getConnectStrBas1()
                            + proxyConfig.getConnectStrFirst()
                            + "\""
                            + string
                            + "\""
                            + proxyConfig.getConnectStrBas2();
        }
        else if(i == size - 1){
            info =
                    proxyConfig.getConnectStrBas1()
                            + proxyConfig.getConnectStrLast();

        }
        else {
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
