package com.backstage.xduchat.controller;

import com.backstage.xduchat.domain.Result;
import com.backstage.xduchat.service.HealthCheckService;
import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @Author: 711lxsky
 * @Description: 服务健康检测控制器
 */

@Api(tags = "服务健康检测")
@RestController
public class HealthCheckController {

    @Resource
    private HealthCheckService healthCheckService;


    @RequestMapping(path = "/health-check", method = RequestMethod.OPTIONS)
    public Result<?> healthCheck() {
        try {
            return healthCheckService.healthCheck();
        }catch (Exception e){
            e.printStackTrace();
            return Result.fail(e);
        }
    }

}
