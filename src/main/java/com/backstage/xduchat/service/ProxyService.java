package com.backstage.xduchat.service;

import reactor.core.publisher.Flux;

/**
 * @Author: 711lxsky
 * @Description:
 */

public interface ProxyService {

    Flux<String> proxyAndSaveRecord(String json);
}
