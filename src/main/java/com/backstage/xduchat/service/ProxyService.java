package com.backstage.xduchat.service;

import com.backstage.xduchat.domain.Result;
import com.fasterxml.jackson.databind.JsonNode;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * @Author: 711lxsky
 * @Description:
 */

public interface ProxyService {

    Flux<String> streamInfo(Map<String, Object> parameters);

    Mono<JsonNode> proxy(Map<String, Object> parameters);

    Flux<String> stream(String json);
}
