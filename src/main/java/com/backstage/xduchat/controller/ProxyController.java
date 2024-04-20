package com.backstage.xduchat.controller;

import com.backstage.xduchat.domain.Result;
import com.backstage.xduchat.service.ProxyService;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * @Author: 711lxsky
 * @Description:
 */

@Api(tags = "数据格式转换")
@RestController
public class ProxyController {

    private final ProxyService proxyService;

    @Autowired
    public ProxyController(ProxyService proxyService)
    {
        this.proxyService = proxyService;
    }

    @PostMapping(path = "/proxy")
    public Mono<JsonNode> proxy(@RequestBody Map<String, Object> parameters){
        return this.proxyService.proxy(parameters);
    }

}
