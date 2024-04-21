package com.backstage.xduchat.service.impl;

import cn.hutool.core.util.StrUtil;
import com.backstage.xduchat.Exception.HttpException;
import com.backstage.xduchat.config.ProxyConfig;
import com.backstage.xduchat.domain.dto.MessageOPENAI;
import com.backstage.xduchat.domain.dto.MessageXDUCHAT;
import com.backstage.xduchat.domain.dto.ParametersXDUCHAT;
import com.backstage.xduchat.service.ProxyService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @Author: 711lxsky
 * @Description: 数据中转并持久化记录
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

    @Override
    public Flux<String> proxyAndSaveRecord(String json) throws HttpException{
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JsonNode jsonObject = objectMapper.readTree(json);
            JsonNode jsonMessages = jsonObject.get(proxyConfig.getParameterMessages());
            if(jsonMessages.isArray()){
                List<MessageOPENAI> messagesOpenai = objectMapper.convertValue(jsonMessages, new TypeReference<>() {});
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
                .onStatus(HttpStatus::isError, response -> response.bodyToMono(String.class)
                        .flatMap(errorBody -> Mono.error(new HttpException(errorBody))))
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
