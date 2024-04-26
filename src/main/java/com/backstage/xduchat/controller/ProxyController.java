package com.backstage.xduchat.controller;

import com.backstage.xduchat.Exception.HttpException;
import com.backstage.xduchat.config.ProxyConfig;
import com.backstage.xduchat.service.ProxyService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * @Author: 711lxsky
 * @Description:
 */

@Api(tags = "数据格式转换")
@RestController
public class ProxyController {

    private final ProxyService proxyService;

    private final ProxyConfig proxyConfig;

    @Autowired
    public ProxyController(ProxyService proxyService, ProxyConfig proxyConfig)
    {
        this.proxyService = proxyService;
        this.proxyConfig = proxyConfig;
    }

    @ApiOperation(value = "数据格式中转，并持久化记录用以后续分析")
    @PostMapping(path = "/v1/chat/completions", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> proxy(@RequestBody String jsonParam){
        try {
            return proxyService.proxyAndSaveRecord(jsonParam);
        }
        catch (HttpException e){
            e.printStackTrace();
            String baseInfo = e.getMessage();
            String fullResponseInfo =
                    proxyConfig.getConnectStrBas1()
                    + proxyConfig.getConnectStrFirst()
                    + "\""
                    + baseInfo
                    + proxyConfig.getConnectStrBas2();
            return Flux.fromArray(new String[]{fullResponseInfo, proxyConfig.getSSEDone()});
        }
    }

}
