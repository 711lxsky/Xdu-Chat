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
@ConfigurationProperties(prefix = "data-settings")
public class DataConfig {

    private String parameterUserid;

}