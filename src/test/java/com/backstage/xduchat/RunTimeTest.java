package com.backstage.xduchat;

import cn.hutool.core.date.BetweenFormatter;
import cn.hutool.core.date.DateUtil;
import com.backstage.xduchat.Utils.ServiceListener;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.Date;

/**
 * @Author: 711lxsky
 * @Description:
 */

@SpringBootTest(classes = XduChatApplication.class)
@Log4j2
public class RunTimeTest {

    private final ServiceListener serviceListener;

    @Autowired
    public RunTimeTest(ServiceListener serviceListener) {
        this.serviceListener = serviceListener;
    }

    @Test
    public void runTimeTest(){
        long serviceStartTime = serviceListener.getServiceStartTime();
        log.info("服务启动时间: {} ms",serviceStartTime);
        long currentTime = System.currentTimeMillis();
        log.info("当前时间: {} ms",currentTime);
        Date startDate = DateUtil.date(serviceStartTime);
        Date currentDate = DateUtil.date(currentTime);
        String between = DateUtil.formatBetween(startDate, currentDate, BetweenFormatter.Level.SECOND);
        log.info("服务已运行: {}",between);
    }

}
