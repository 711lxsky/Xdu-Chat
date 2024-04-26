package com.backstage.xduchat.service;

import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @Author: 711lxsky
 * @Description:
 */

public interface ProxyService {

    Flux<String> proxyAndSaveRecord(String json);

    Boolean needStream(String jsonParam);
}
