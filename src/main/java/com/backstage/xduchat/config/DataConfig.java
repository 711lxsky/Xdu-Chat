package com.backstage.xduchat.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @Author: 711lxsky
 */

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "data-settings")
public class DataConfig {

    private String parameterUserid;

    private String parameterRecordId;

    private String parameterStream;

    private String normalNotSSEResponseConnectStr1 = "{\n" +
            "    \"id\": \"1\",\n" +
            "    \"object\": \"2\",\n" +
            "    \"created\": \"3\",\n" +
            "    \"model\": \"gpt-3.5-turbo\",\n" +
            "    \"usage\": {\n" +
            "        \"prompt_tokens\": 0,\n" +
            "        \"completion_tokens\": 0,\n" +
            "        \"total_tokens\": 0\n" +
            "    },\n" +
            "    \"choices\": [\n" +
            "        {\n" +
            "            \"message\": {\n" +
            "                \"role\": \"assistant\",\n" +
            "                \"content\": \"";

    private String normalNotSSEResponseConnectStr2 = "\"\n" +
            "            },\n" +
            "            \"finish_reason\": \"stop\",\n" +
            "            \"index\": 0\n" +
            "        }\n" +
            "    ]\n" +
            "}";
}
