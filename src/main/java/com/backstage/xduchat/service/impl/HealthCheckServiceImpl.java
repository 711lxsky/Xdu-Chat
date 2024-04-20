package com.backstage.xduchat.service.impl;

import cn.hutool.core.date.BetweenFormatter;
import cn.hutool.core.date.DateUtil;
import com.backstage.xduchat.Utils.ServiceListener;
import com.backstage.xduchat.config.ServiceConfig;
import com.backstage.xduchat.domain.Result;
import com.backstage.xduchat.domain.vo.HealthInfo;
import com.backstage.xduchat.service.HealthCheckService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;

/**
 * @Author: 711lxsky
 * @Description:
 */

@Service
public class HealthCheckServiceImpl implements HealthCheckService {

//    private final ApplicationService applicationService;
    private final ServiceListener serviceListener;

    @Resource
    private ServiceConfig serviceConfig;

    @Autowired
    public HealthCheckServiceImpl(ServiceListener serviceListener) {
        this.serviceListener = serviceListener;
    }

    @Override
    public Result<?> healthCheck() {
        String serviceName = serviceConfig.getServiceName();
        long serviceStartTime = serviceListener.getServiceStartTime();
        long currentTime = System.currentTimeMillis();
        Date startDate = DateUtil.date(serviceStartTime);
        Date currentDate = DateUtil.date(currentTime);
        HealthInfo healthInfo = new HealthInfo(serviceName, currentTime - serviceStartTime);
        String between = DateUtil.formatBetween(startDate, currentDate, BetweenFormatter.Level.SECOND);
        String message = "服务已运行" + between + "，当前时间：" + DateUtil.format(currentDate, "yyyy-MM-dd HH:mm:ss");
        return Result.success(message, healthInfo);
    }
}
