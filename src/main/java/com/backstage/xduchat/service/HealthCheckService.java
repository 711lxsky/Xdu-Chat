package com.backstage.xduchat.service;

import com.backstage.xduchat.domain.Result;

/**
 * @Author: 711lxsky
 * @Description:
 */

public interface HealthCheckService {
    Result<?> healthCheck();
}
