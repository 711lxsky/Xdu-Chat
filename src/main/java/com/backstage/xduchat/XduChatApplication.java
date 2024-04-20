package com.backstage.xduchat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import springfox.documentation.swagger2.annotations.EnableSwagger2WebMvc;

/**
 * @Author: 711lxsky
 * @Description: 启动类
 */
@SpringBootApplication
@EnableSwagger2WebMvc
public class XduChatApplication {

    public static void main(String[] args) {
        SpringApplication.run(XduChatApplication.class, args);
    }

}
