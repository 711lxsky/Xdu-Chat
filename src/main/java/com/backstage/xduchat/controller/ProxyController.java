package com.backstage.xduchat.controller;

import com.backstage.xduchat.Exception.HttpException;
import com.backstage.xduchat.config.ProxyConfig;
import com.backstage.xduchat.service.ProxyService;
import com.backstage.xduchat.service.RealProxyService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;

/**
 * @Author: 711lxsky
 * @Description:
 */

@Api(tags = "数据格式转换")
@RestController
public class ProxyController {

    private final ProxyService proxyService;

    private final ProxyConfig proxyConfig;

    private final RealProxyService realProxyService;

    @Autowired
    public ProxyController(ProxyService proxyService, ProxyConfig proxyConfig, RealProxyService realProxyService)
    {
        this.proxyService = proxyService;
        this.proxyConfig = proxyConfig;
        this.realProxyService  = realProxyService;
    }

    @ApiOperation(value = "数据中传重构")
    @PostMapping(path = "/proxy")
    public Object proxy(@RequestBody String jsonParam){
        return realProxyService.proxy(jsonParam);
    }


    @ApiOperation(value = "数据格式中转，并持久化记录用以后续分析")
    @PostMapping(path = "/v1/chat/completions"
            , produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public Object proxy1(@RequestBody String jsonParam){
        try {
            return proxyService.proxyAndSaveRecord(jsonParam);
        }
        catch (HttpException e){
            e.printStackTrace();
            if(!proxyService.needStream(jsonParam)){
                return ResponseEntity.status(HttpStatus.CONFLICT).contentType(MediaType.APPLICATION_JSON).body(e.getMessage());
            }
            String baseInfo = e.getMessage();
            String fullResponseInfo =
                    proxyConfig.getConnectStrBas1()
                    + proxyConfig.getConnectStrFirst()
                    + "\""
                    + baseInfo
                    + proxyConfig.getConnectStrBas2();
            Flux<String> stringFlux = Flux.fromArray(new String[]{fullResponseInfo, proxyConfig.getSSEDone()});
            return ResponseEntity.ok().body(stringFlux);
        }
    }

}
