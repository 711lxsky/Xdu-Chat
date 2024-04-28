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
@ConfigurationProperties(prefix = "proxy-settings")
public class ProxyConfig {

    private String parameterMessages;

    private String parameterRoleUser;

    private String parameterRoleAssistant;

    private String parameterRoleSystem;

    private String parameterRoleHUMAN;

    private String parameterRoleBOT;

    private String XduchatApiUrl;

    private Integer requestTimeout;

    private Long SSESendTime;

    private String responseData;

    private String responseDataResponse;

    private String repeatRequest;

    private String XduchatApiUrlNew;

    private String SSEData = "data: ";

    private String SSENewLineDouble = "\n\n";

    private String connectStrBas1 = " {\"id\": \"1\",\"object\": \"2\",\"created\": \"3\",\"model\": \"gpt-3.5-turbo\",\"system_fingerprint\": \"4\",\"choices\":[{\"index\":0,\"delta\": ";

    private String connectStrFirst = "{\"role\":\"assistant\",\"content\": ";

    private String connectStrIndexMid = "{\"content\": ";

    private String connectStrLast = "{\"content\":\"\"},\"logprobs\": null,\"finish_reason\": \"stop\"}]}";

    private String connectStrBas2 = "},\"logprobs\":null,\"finish_reason\":null}]}";

    private String SSEDone = "[DONE]";
}
