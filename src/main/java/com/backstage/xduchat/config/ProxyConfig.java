package com.backstage.xduchat.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @Author: 711lxsky
 * @Description:
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

    private String responseData;

    private String responseDataResponse;
}
