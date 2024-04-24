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

    @Autowired
    public ProxyServiceImpl(ProxyConfig proxyConfig, WebClient webClient, DataConfig dataConfig, JsonUtil jsonUtil, GeneralRecordService generalRecordService){
        this.proxyConfig = proxyConfig;
        this.webClient =  webClient;
        this.dataConfig = dataConfig;
        this.jsonUtil = jsonUtil;
        this.generalRecordService = generalRecordService;
    }

    @Override
    public Flux<String> proxyAndSaveRecord(String jsonParametersStr) throws HttpException{
        // 逐级校验数据
        JsonNode jsonParameters = jsonUtil.getJsonNode(jsonParametersStr);
        if(Objects.isNull(jsonParameters)){
            throw new HttpException(ExceptionConstant.ParameterNull.getMessage_EN());
        }
        JsonNode jsonMessages = jsonParameters.get(proxyConfig.getParameterMessages());
        if(Objects.isNull(jsonMessages)){
            throw new HttpException(ExceptionConstant.MassagesNull.getMessage_EN());
        }
        JsonNode jsonUserId = jsonParameters.get(dataConfig.getParameterUserid());
        if(Objects.isNull(jsonUserId)){
            throw new HttpException(ExceptionConstant.UserIdIsNull.getMessage_EN());
        }
        String userId = jsonUserId.asText();
        if(! StringUtils.hasText(userId)){
            String info =
                    proxyConfig.getConnectStrBas1()
                            + proxyConfig.getConnectStrFirst()
                            + "\""
                            + ExceptionConstant.UserIdIsNull.getMMessage_ZH() + ", 请进行统一身份认证登录 ！ "
                            + "\""
                            + proxyConfig.getConnectStrBas2();
            Flux<String> infos = Flux.fromArray(new String[]{info, proxyConfig.getSSEDone()});
            return infos;
        }
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
                                GeneralRecord generalRecord = new GeneralRecord(userId, new Date(System.currentTimeMillis()), jsonGeneralRecords);
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
                .timeout(Duration.ofSeconds(proxyConfig.getRequestTimeout()));
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
