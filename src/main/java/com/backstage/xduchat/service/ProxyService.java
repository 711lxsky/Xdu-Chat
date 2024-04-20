package com.backstage.xduchat.service;

import com.backstage.xduchat.domain.Result;
import com.fasterxml.jackson.databind.JsonNode;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * @Author: 711lxsky
 * @Description:
 */

public interface ProxyService {
    Mono<JsonNode> proxy(Map<String, Object> parameters);
}
