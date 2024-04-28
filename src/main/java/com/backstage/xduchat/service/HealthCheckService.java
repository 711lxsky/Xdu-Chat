package com.backstage.xduchat.service;

import com.backstage.xduchat.domain.Result;

/**
 * @Author: 711lxsky
 */

public interface HealthCheckService {
    Result<?> healthCheck();
}
