package com.backstage.xduchat.service.impl;

import cn.hutool.core.util.StrUtil;
import com.backstage.xduchat.Exception.HttpException;
import com.backstage.xduchat.Utils.JsonUtil;
import com.backstage.xduchat.config.ProxyConfig;
import com.backstage.xduchat.domain.dto.MessageOPENAI;
import com.backstage.xduchat.domain.dto.MessageXDUCHAT;
import com.backstage.xduchat.domain.dto.ParametersXDUCHAT;
import com.backstage.xduchat.domain.entity.GeneralRecord;
import com.backstage.xduchat.service.DialogueTimeService;
import com.backstage.xduchat.service.GeneralRecordService;
import com.backstage.xduchat.setting_enum.DialogueTimeConstant;
import com.backstage.xduchat.setting_enum.ExceptionConstant;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Log4j2
@Service
public class RequestForXDUCHAT {

    @Resource
    private JsonUtil jsonUtil;

    @Resource
    private ProxyConfig proxyConfig;

    @Resource
    private RestTemplate restTemplate;

    @Resource
    private GeneralRecordService generalRecordService;

    @Resource
    private DialogueTimeService dialogueTimeService;

    public String requestForXDUCHAT(String userId, String recordId, JsonNode jsonMessages, int curDialogueTime) throws HttpException {
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
                String realResponse = xduchatResponse.get(proxyConfig.getXdeResponse()).asText();
//                log.info("real-response: {}", realResponse);
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
