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
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @Author: 711lxsky
 * @Description: 数据中转并持久化记录
 */

@Log4j2
@Service
public class MockProxyService {

    private final ProxyConfig proxyConfig;

    private final WebClient webClient;

    private final DataConfig dataConfig;

    private final JsonUtil jsonUtil;

    private final GeneralRecordService generalRecordService;

    private final ConcurrentHashMap<String, Flux<String>> proxyRecords;

    private final Map<String, Sinks.Many<String>> proxyStatus;

//    private final Map<String, AtomicBoolean> processingStatus;
//
//    private final Map<String, Sinks.Empty<Void>> proxyCompletions;

    private final Map<String, ReentrantLock> proxyLocks;
//
//    private final Map<String, Condition> proxyConditions;

    private final Map<String, AtomicReference<Sinks.Empty<Void>>> proxyCompletions;
    
    @Autowired
    public MockProxyService(ProxyConfig proxyConfig, WebClient webClient, DataConfig dataConfig, JsonUtil jsonUtil, GeneralRecordService generalRecordService){
        this.proxyConfig = proxyConfig;
        this.webClient =  webClient;
        this.dataConfig = dataConfig;
        this.jsonUtil = jsonUtil;
        this.generalRecordService = generalRecordService;
        this.proxyRecords = new ConcurrentHashMap<>();
        this.proxyStatus = new ConcurrentHashMap<>();
        this.proxyLocks = new ConcurrentHashMap<>();
        this.proxyCompletions = new ConcurrentHashMap<>();
    }

    private boolean judgeJsonDataIsNull(JsonNode jsonNode){
        if(Objects.isNull(jsonNode)){
            return true;
        }
        String jsonNodeText = jsonUtil.parseJsonNodeToString(jsonNode);
        return !StringUtils.hasText(jsonNodeText);
    }


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
            throw new HttpException(ExceptionConstant.UserIdIsNull.getMessage_ZH());
        }
        String userId = jsonUserId.asText();
        // record_id
        JsonNode jsonRecordId = jsonParameters.get(dataConfig.getParameterRecordId());
        if(this.judgeJsonDataIsNull(jsonRecordId)){
            throw new HttpException(ExceptionConstant.RecordIdIsNull.getMessage_ZH());
        }
        String recordId = jsonRecordId.asText();
        // 将 userId 和 recordId 拼接组成 唯一标识符
        String identifier = userId + recordId;
        proxyLocks.computeIfAbsent(identifier, key -> new ReentrantLock());
        proxyCompletions.computeIfAbsent(identifier, key -> new AtomicReference<>());
        ReentrantLock lock = proxyLocks.get(identifier);
        AtomicReference<Sinks.Empty<Void>> completionSignalRef = proxyCompletions.get(identifier);
        boolean acquired = lock.tryLock();
        return  Flux.<String>create(sink -> {
            log.info("current locks: {}", proxyLocks.entrySet().toString());
            log.info("this get lock :{} , identifier {}", acquired + lock.toString(), identifier);
            try {
                if (!acquired) {
                    completionSignalRef.get().asMono().subscribe(
                            nullValue -> sink.error(new HttpException(ExceptionConstant.RequestRepeat.getMessage_ZH())),
                            error -> sink.error(new HttpException(ExceptionConstant.RequestRepeat.getMessage_ZH())),
                            () -> sink.error(new HttpException(ExceptionConstant.RequestRepeat.getMessage_ZH()))
//                            sink::error,
//                            sink::complete
                    );
                } else {
                    try {
                        this.internalProxy(userId, recordId, jsonMessages)
                                .subscribe(
                                        result -> {
                                            sink.next(result);
                                        },
                                        error -> {
                                            sink.error(error);
                                        },
                                        () -> {
                                            sink.complete();
                                        }
                                );
                    } finally {
                        completionSignalRef.set(Sinks.empty());
                        completionSignalRef.get().tryEmitEmpty();
                        lock.unlock();
                    }
                }
            } catch (Exception e) {
                sink.error(e);
                if (acquired) {
                    log.info("---=====-----====== lock out");
                    lock.unlock();
                }
            }
        }).doFinally(signalType -> {
            proxyLocks.remove(identifier);
            proxyCompletions.remove(identifier);
        });
        /*
        AtomicBoolean isProcessing = processingStatus.computeIfAbsent(identifier, key -> new AtomicBoolean(false));
        Sinks.Empty<Void> completionSink = proxyCompletions.computeIfAbsent(identifier, key -> Sinks.empty());
        if(! isProcessing.compareAndSet(false, true)){
            return Flux.from(completionSink.asMono()
                    .thenReturn(ExceptionConstant.RequestRepeat.getMessage_ZH())
                    .doFinally(signalType -> proxyCompletions.remove(identifier)));
        }
        return this.internalProxy(userId, recordId, jsonMessages)
                .doOnTerminate(() -> {
                    isProcessing.set(false);
                    completionSink.tryEmitEmpty();
                });
        /*
        return Flux.create(sink -> {
            if(isProcessing.compareAndSet(false, true)) {
                Sinks.Many<String> currentSink = Sinks.many().replay().latest();
                proxyStatus.put(identifier, currentSink);

                currentSink.asFlux().subscribe(sink::next, sink::error, sink::complete);

                this.internalProxy(userId, recordId, jsonMessages)
                        .doOnTerminate( () -> {
                            isProcessing.set(false);
                            proxyStatus.remove(identifier);
                        })
                        .subscribe(
                                currentSink::tryEmitNext,
                                currentSink::tryEmitError,
                                currentSink::tryEmitComplete
                        );
            }
            else {
                sink.error(new HttpException(ExceptionConstant.RequestRepeat.getMessage_ZH()));
            }
        });
        /*
        Sinks.Many<String> currentSink = proxyStatus.computeIfAbsent(identifier, key -> Sinks.many().replay().latest());
        ReentrantLock lock = proxyLocks.computeIfAbsent(identifier, key -> new ReentrantLock());
        Condition condition = proxyConditions.computeIfAbsent(identifier, key -> lock.newCondition());
        lock.lock();
        try {
            if(currentSink.currentSubscriberCount() > 0){
                try {
                    condition.await();
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return Flux.error(new HttpException(ExceptionConstant.ThreadInterruptError.getMessage_ZH()));
                }
                throw new HttpException(ExceptionConstant.RequestRepeat.getMessage_ZH());
//                return Flux.error(new HttpException(ExceptionConstant.RequestRepeat.getMessage_ZH()));
            }
            else {
                return Flux.create(
                        sink -> {
                            currentSink.asFlux().subscribe(sink::next, sink::error, sink::complete);
                            this.internalProxy(userId, recordId, jsonMessages)
                                    .flatMap(proxyResult -> Mono.just(proxyResult))
                                    .subscribe(
                                            currentSink::tryEmitNext,
                                            sink::error,
                                            () -> {
                                                currentSink.tryEmitComplete();
                                                proxyStatus.remove(identifier);
                                                proxyLocks.remove(identifier);
                                                proxyConditions.remove(identifier);
                                                condition.signalAll();
                                            }
                                    );
                        });
            }
        }
        finally {
            lock.unlock();
        }
        /*
        return proxyRecords.computeIfAbsent(identifier, key -> {
            Flux<String> proxyResult = this.internalProxy(userId, recordId, jsonMessages)
                    .cache()
                    .doFinally(signalType -> proxyRecords.remove(identifier));
            return proxyResult;
        });
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

    public Boolean needStream(String jsonParam) {
        JsonNode jsonParameters = jsonUtil.getJsonNode(jsonParam);
        if(this.judgeJsonDataIsNull(jsonParameters)){
            throw new HttpException(ExceptionConstant.ParameterNull.getMessage_ZH());
        }
        JsonNode jsonStream = jsonParameters.get(dataConfig.getParameterStream());
        if(Objects.isNull(jsonStream)){
            return false;
        }
        return jsonStream.asBoolean();
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
