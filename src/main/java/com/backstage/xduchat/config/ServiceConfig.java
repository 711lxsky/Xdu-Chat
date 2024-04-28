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
@ConfigurationProperties(prefix = "service-settings")
public class ServiceConfig {

    private String serviceName;

}
