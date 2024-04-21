package com.backstage.xduchat.service.impl;

import cn.hutool.Hutool;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSON;
import com.backstage.xduchat.Exception.HttpException;
import com.backstage.xduchat.config.ProxyConfig;
import com.backstage.xduchat.domain.Result;
import com.backstage.xduchat.domain.dto.MessageOPENAI;
import com.backstage.xduchat.domain.dto.MessageXDUCHAT;
import com.backstage.xduchat.domain.dto.ParametersXDUCHAT;
import com.backstage.xduchat.service.ProxyService;
import com.backstage.xduchat.setting_enum.ExceptionConstant;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * @Author: 711lxsky
 * @Description:
 */

@Log4j2
@Service
public class ProxyServiceImpl implements ProxyService {

    private final ProxyConfig proxyConfig;

    private final WebClient webClient;

    @Autowired
    public ProxyServiceImpl(ProxyConfig proxyConfig, WebClient webClient) {
        this.proxyConfig = proxyConfig;
        this.webClient =  webClient;
    }

    public Flux<String> streamInfo(Map<String, Object> parameters){
        List<MessageOPENAI> messagesOpenai = (List<MessageOPENAI>) parameters.get(proxyConfig.getParameterMessages());
        List<MessageXDUCHAT> messagesXduchat = this.convertMessage(messagesOpenai);
        ParametersXDUCHAT parametersXDUCHAT = new ParametersXDUCHAT(messagesXduchat, null);
        return this.requestForXDUCHAT(parametersXDUCHAT)
                .flatMapMany(jsonString -> {
                   String responseInfo = jsonString;
                   return Flux.fromStream(IntStream.range(0, responseInfo.length()).mapToObj(
                           i -> String.valueOf(responseInfo.charAt(i)))
                           .map(s -> new String(s.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8)));
                });
    }

    @Override
    public Mono<JsonNode> proxy(Map<String, Object> parameters) throws HttpException{
        return null;
//        try {
//            Object object = parameters.get(proxyConfig.getParameterMessages());
//            List<MessageXDUCHAT> tmp = (List<MessageXDUCHAT>) object;
//            List<MessageOPENAI> messagesOpenai = (List<MessageOPENAI>) parameters.get(proxyConfig.getParameterMessages());
//            List<MessageXDUCHAT> messagesXduchat = this.convertMessage(messagesOpenai);
//            ParametersXDUCHAT parametersXDUCHAT = new ParametersXDUCHAT(messagesXduchat, null);
//            // 下面准备往XDU-CHAT的接口去做请求，拿到之后再流式返回
//            Mono<JsonNode> jsonNodeMono = requestForXDUCHAT(parametersXDUCHAT);
//
//            return this.requestForXDUCHAT(parametersXDUCHAT).doOnNext(
//
//                    respose -> {
//
//                        String usefulResponse = respose.get(proxyConfig.getResponseData()).get(proxyConfig.getResponseDataResponse()).asText();
//                        String[] split = usefulResponse.split("");
//                    }
//            );
//
//        }
//        catch (ClassCastException e) {
//            throw new HttpException(ExceptionConstant.ParameterError.getMessage_EN());
//        }
    }

    @Override
    public Flux<String> stream(String json) throws HttpException{
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JsonNode jsonObject = objectMapper.readTree(json);
            JsonNode jsonMessages = jsonObject.get(proxyConfig.getParameterMessages());
            if(jsonMessages.isArray()){
                List<MessageOPENAI> messagesOpenai = objectMapper.convertValue(jsonMessages, new TypeReference<List<MessageOPENAI>>() {});
                List<MessageXDUCHAT> messagesXduchat = this.convertMessage(messagesOpenai);
                List<String> params = new ArrayList<>();
                ParametersXDUCHAT parametersXDUCHAT = new ParametersXDUCHAT(messagesXduchat, params);
                return this.requestForXDUCHAT(parametersXDUCHAT)
                        .flatMapMany(
                                jsonString -> {
                                    String [] strings = jsonString.split("");
                                    List<String> responseInfos = new ArrayList<>();
                                    Collections.addAll(responseInfos, strings);
                                    responseInfos.add(proxyConfig.getSSEDone());
                                    int lenInfo = responseInfos.size();
                                    Flux<String> stringFlux = Flux.fromIterable(responseInfos);
                                    Flux<Long> intervalFlux = Flux.interval(Duration.ofMillis(100));
                                    return Flux.zip(stringFlux, intervalFlux, (string, index) -> {
                                        if(index == 0){
                                            return proxyConfig.getSSEData()
                                                    + proxyConfig.getConnectStrBas1()
                                                    + proxyConfig.getConnectStrFirst()
                                                    + string
                                                    + proxyConfig.getConnectStrBas2();
//                                                    + proxyConfig.getSSENewLineDouble();
                                        }
                                        else if (index == lenInfo - 2){
                                            return proxyConfig.getSSEData()
                                                    + proxyConfig.getConnectStrBas1()
                                                    + proxyConfig.getConnectStrLast();
//                                                    + proxyConfig.getSSENewLineDouble();
                                        }
                                        else if(index == lenInfo - 1){
                                            return proxyConfig.getSSEDone();
                                        }
                                        else {
                                            return proxyConfig.getSSEData()
                                                    + proxyConfig.getConnectStrBas1()
                                                    + string
                                                    + proxyConfig.getConnectStrBas2();
//                                                    + proxyConfig.getSSENewLineDouble();
                                        }
                                    });
                                }
                        );

//                        .flatMapMany(jsonString -> Flux.fromArray(jsonString.split("")))
//                        .zipWith(Flux.interval(Duration.ofMillis(100)))
//                        .map(Tuple2::getT1);
//                        .flatMapMany(jsonNode -> {
//                            System.out.println("this is log" +jsonNode.toString());
//                            log.info("this is log" + jsonNode.toString());
//                            String[] strings = jsonNode.split("");
//                            String responseInfo = jsonNode;
//                            Flux<String> StringFlux = Flux.fromArray(strings);
//                            Flux<Long> intervalFlux = Flux.interval(Duration.ofMillis(100));
//                            return Flux.zip(StringFlux, intervalFlux, (string, time) -> string);
//                            return Flux.fromStream(IntStream.range(0, responseInfo.length()).mapToObj(
//                                            i -> String.valueOf(responseInfo.charAt(i)))
//                                    .map(s -> new String(s.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8)));
            }

        } catch (JsonProcessingException e) {
            throw new HttpException(e.getMessage());
        }
        return null;
    }

    private Mono<String> requestForXDUCHAT(ParametersXDUCHAT parametersXDUCHAT) throws HttpException {

        log.info(parametersXDUCHAT.toString());
        return webClient.post()
                .uri(proxyConfig.getXduchatApiUrlNew())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(parametersXDUCHAT)
                .retrieve()
                .onStatus(HttpStatus::isError, response -> {
                    return response.bodyToMono(String.class)
                            .flatMap(errorBody -> Mono.error(new HttpException(errorBody)));
                })
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(proxyConfig.getRequestTimeout()));
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
}
