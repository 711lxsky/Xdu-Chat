package com.backstage.xduchat.config;

import com.backstage.xduchat.setting_enum.WebSetting;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @Author: 711lxsky
 * @Date: 2024-03-16
 */

@Configuration(proxyBeanMethods = false)
public class WebConfig implements WebMvcConfigurer {

    @Resource
    private ProxyConfig proxyConfig;

    @Bean
    public ExecutorService taskExecutor() {
        return Executors.newCachedThreadPool();
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate(this.clientHttpRequestFactory());
    }

    private ClientHttpRequestFactory clientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setReadTimeout(proxyConfig.getRequestTimeout() * 1000);
        factory.setConnectTimeout(proxyConfig.getRequestTimeout() * 1000);
        return factory;
    }


    @Bean
    public WebClient webClient(WebClient.Builder builder){
        return builder
                .baseUrl(proxyConfig.getXduchatApiUrlNew())
                .build();
    }

    /**
     * 跨域 注意： 跨域问题涉及安全性问题，这里提供的是最方便简单的配置，任何host和任何方法都可跨域 但在实际场景中，这样做，无疑很危险，所以谨慎选择开启或者关闭
     * 如果切实需要，请咨询相关安全人员或者专家进行配置
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping(WebSetting.getMappingAll())
                .allowedOriginPatterns(WebSetting.getAll())
                .allowedMethods(WebSetting.getAllowCordsMethods())
                .allowCredentials(true)
                .maxAge(WebSetting.getMaxAge())
                .allowedHeaders(WebSetting.getAll());
    }

    /*
      SQL过滤器避免SQL注入
      @param resolvers 过滤器
     */
//    @Override
//    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
//        // resolvers.add(new SqlFilterArgumentResolver());
//    }
}
