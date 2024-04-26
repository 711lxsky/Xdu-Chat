package com.backstage.xduchat.service.impl;

import cn.hutool.core.util.StrUtil;
import com.backstage.xduchat.Exception.HttpException;
import com.backstage.xduchat.Utils.JsonUtil;
import com.backstage.xduchat.config.DataConfig;
import com.backstage.xduchat.config.ProxyConfig;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

/**
 * @Author: 711lxsky
 * @Description: 数据中转并持久化记录
 */

@Log4j2
@Service
public class ProxyServiceImpl implements ProxyService {

    private final ProxyConfig proxyConfig;

    private final WebClient webClient;

    private final DataConfig dataConfig;

    private final JsonUtil jsonUtil;

    private final GeneralRecordService generalRecordService;

    private final ConcurrentHashMap<String, Flux<String>> proxyRecords;

    @Autowired
    public ProxyServiceImpl(ProxyConfig proxyConfig, WebClient webClient, DataConfig dataConfig, JsonUtil jsonUtil, GeneralRecordService generalRecordService){
        this.proxyConfig = proxyConfig;
        this.webClient =  webClient;
        this.dataConfig = dataConfig;
        this.jsonUtil = jsonUtil;
        this.generalRecordService = generalRecordService;
        this.proxyRecords = new ConcurrentHashMap<>();
    }

    private boolean judgeJsonDataIsNull(JsonNode jsonNode){
        if(Objects.isNull(jsonNode)){
            return true;
        }
        String jsonNodeText = jsonUtil.parseJsonNodeToString(jsonNode);
        return !StringUtils.hasText(jsonNodeText);
    }

    @Override
    public Flux<String> proxyAndSaveRecord(String jsonParametersStr) throws HttpException{
        // 逐级校验数据
        // 最外层参数
        JsonNode jsonParameters = jsonUtil.getJsonNode(jsonParametersStr);
        if(this.judgeJsonDataIsNull(jsonParameters)){
            throw new HttpException(ExceptionConstant.ParameterNull.getMessage_ZH());
        }
        // messages
        JsonNode jsonMessages = jsonParameters.get(proxyConfig.getParameterMessages());
        if(this.judgeJsonDataIsNull(jsonMessages)){
            throw new HttpException(ExceptionConstant.MassagesNull.getMessage_ZH());
        }
        // uid
        JsonNode jsonUserId = jsonParameters.get(dataConfig.getParameterUserid());
        if(this.judgeJsonDataIsNull(jsonUserId)){
            throw new HttpException(ExceptionConstant.UserIdIsNull.getMessage_ZH() + ", 请进行统一身份认证登录 ！ ");
        }
        String userId = jsonUserId.asText();
        // record_id
        JsonNode jsonRecordId = jsonParameters.get(dataConfig.getParameterRecordId());
        if(this.judgeJsonDataIsNull(jsonRecordId)){
            throw new HttpException(ExceptionConstant.RecordIdIsNull.getMessage_ZH() + ", 请检查参数 ！ ");
        }
        String recordId = jsonRecordId.asText();
        // 将 userId 和 recordId 拼接组成 唯一标识符
        String identifier = userId + recordId;
        return proxyRecords.computeIfAbsent(identifier, key -> {
            Flux<String> proxyResult = this.internalProxy(userId, recordId, jsonMessages)
                    .cache()
                    .doFinally(signalType -> proxyRecords.remove(identifier));
            return proxyResult;
        });
        /*
        CompletableFuture<Flux<String>> proxyResult = this.proxyRecords.computeIfAbsent(identifier, key -> new CompletableFuture<>());
        try {
            if (!proxyResult.isDone()) {
                if (proxyRecords.get(identifier) == proxyResult) {
                    try {
                        Flux<String> proxyRes = this.internalProxy(userId, recordId, jsonMessages);
                        proxyResult.complete(proxyRes);
                    } finally {
                        this.proxyRecords.remove(identifier);
                    }
                }
            }
            return proxyResult.get();
        }
        catch (InterruptedException | ExecutionException e) {
            throw new HttpException(e.getMessage());
        }
        */
    }

    private Flux<String> internalProxy(String userId, String recordId, JsonNode jsonMessages) throws HttpException{
        if(jsonMessages.isArray()){
            List<MessageOPENAI> messagesOpenai = jsonUtil.getObjectMapper().convertValue(jsonMessages, new TypeReference<>() {});
            List<MessageXDUCHAT> messagesXduchat = this.convertMessage(messagesOpenai);
            // 重新构造参数
            List<String> params = new ArrayList<>();
            ParametersXDUCHAT parametersXDUCHAT = new ParametersXDUCHAT(messagesXduchat, params);
            return this.requestForXDUCHAT(parametersXDUCHAT)
                    .flatMapMany(
                            jsonString -> {
                                // 拿到返回值之后处理
                                MessageOPENAI responseMessage = new MessageOPENAI(proxyConfig.getParameterRoleAssistant(), jsonString);
                                messagesOpenai.add(responseMessage);
                                String jsonGeneralRecords = jsonUtil.toJson(messagesOpenai);
                                GeneralRecord generalRecord = new GeneralRecord(userId, recordId, new Date(System.currentTimeMillis()), jsonGeneralRecords);
                                // 持久化
                                generalRecordService.save(generalRecord);
                                // 每个字符分割
                                String [] strings = jsonString.split("");
                                log.info("strings: {}",Arrays.toString(strings));
                                List<String> responseInfos = new ArrayList<>();
                                Collections.addAll(responseInfos, strings);
                                responseInfos.add(proxyConfig.getSSEDone());
                                int lenInfo = responseInfos.size();
                                // 设置发送字符串
                                Flux<String> stringFlux = Flux.fromIterable(responseInfos);
                                // 设置发送间隔
                                Flux<Long> intervalFlux = Flux.interval(Duration.ofMillis(100));
                                return Flux.zip(stringFlux, intervalFlux, (string, index) -> {
                                    if(index == 0){
                                        String info =
//                                                proxyConfig.getSSEData() +
                                                proxyConfig.getConnectStrBas1()
                                                        + proxyConfig.getConnectStrFirst()
                                                        + "\""
                                                        + string
                                                        + "\""
                                                        + proxyConfig.getConnectStrBas2();
//                                                    + proxyConfig.getSSENewLineDouble();
//                                        log.info("index: {} info: {}", index,  info);
                                        return info;
                                    }
                                    else if (index == lenInfo - 2){
                                        String info =
//                                                proxyConfig.getSSEData() +
                                                proxyConfig.getConnectStrBas1()
                                                        + proxyConfig.getConnectStrLast();
//                                                    + proxyConfig.getSSENewLineDouble();
//                                        log.info("index: {} info: {}", index,  info);
                                        return info;
                                    }
                                    else if(index == lenInfo - 1){
                                        return proxyConfig.getSSEDone();
                                    }
                                    else {
                                        String info =
//                                                proxyConfig.getSSEData() +
                                                proxyConfig.getConnectStrBas1()
                                                        + proxyConfig.getConnectStrIndexMid()
                                                        + "\""
                                                        + string
                                                        + "\""
                                                        + proxyConfig.getConnectStrBas2();
//                                                    + proxyConfig.getSSENewLineDouble();
//                                        log.info("index: {} info: {}", index,  info);
                                        return info;
                                    }
                                });
                            }
                    );
        }
        throw new HttpException(ExceptionConstant.DataError.getMessage_EN());
    }

    /**
     * @Author: 711lxsky
     * @Description: 向 XDUCHAT 请求
     */
    private Mono<String> requestForXDUCHAT(ParametersXDUCHAT parametersXDUCHAT) throws HttpException {
        log.info(parametersXDUCHAT.toString());
        return webClient.post()
                .uri(proxyConfig.getXduchatApiUrlNew())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(parametersXDUCHAT)
                .retrieve()
                .onStatus(HttpStatus::isError, response -> response.bodyToMono(String.class)
                        .flatMap(errorBody -> Mono.error(new HttpException(errorBody))))
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(proxyConfig.getRequestTimeout()),
                        Mono.just(ExceptionConstant.TimeOut.getMessage_ZH()));
    }

    /**
     * @Author: 711lxsky
     * @Description: message 格式转换
     */
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
}
