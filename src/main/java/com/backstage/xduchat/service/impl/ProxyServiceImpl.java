package com.backstage.xduchat.service.impl;

import cn.hutool.core.util.StrUtil;
import com.backstage.xduchat.Exception.HttpException;
import com.backstage.xduchat.config.ProxyConfig;
import com.backstage.xduchat.domain.Result;
import com.backstage.xduchat.domain.dto.MessageOPENAI;
import com.backstage.xduchat.domain.dto.MessageXDUCHAT;
import com.backstage.xduchat.domain.dto.ParametersXDUCHAT;
import com.backstage.xduchat.service.ProxyService;
import com.backstage.xduchat.setting_enum.ExceptionConstant;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @Author: 711lxsky
 * @Description:
 */

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
    public Mono<JsonNode> proxy(Map<String, Object> parameters) throws HttpException{
        try {
            List<MessageOPENAI> messagesOpenai = (List<MessageOPENAI>) parameters.get(proxyConfig.getParameterMessages());
            List<MessageXDUCHAT> messagesXduchat = this.convertMessage(messagesOpenai);
            ParametersXDUCHAT parametersXDUCHAT = new ParametersXDUCHAT(messagesXduchat, null);
            // 下面准备往XDU-CHAT的接口去做请求，拿到之后再流式返回
            Mono<JsonNode> jsonNodeMono = requestForXDUCHAT(parametersXDUCHAT);

            return this.requestForXDUCHAT(parametersXDUCHAT).doOnNext(
                    respose -> {
                        String usefulResponse = respose.get(proxyConfig.getResponseData()).get(proxyConfig.getResponseDataResponse()).asText();
                        String[] split = usefulResponse.split("");
                    }
            );

        }
        catch (ClassCastException e) {
            throw new HttpException(ExceptionConstant.ParameterError.getMessage_EN());
        }
    }

    private Mono<JsonNode> requestForXDUCHAT(ParametersXDUCHAT parametersXDUCHAT) throws HttpException {
        return webClient.post()
                .uri(proxyConfig.getXduchatApiUrl())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(parametersXDUCHAT)
                .retrieve()
                .onStatus(HttpStatus::isError, response -> {
                    return response.bodyToMono(String.class)
                            .flatMap(errorBody -> Mono.error(new HttpException(errorBody)));
                })
                .bodyToMono(JsonNode.class)
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
