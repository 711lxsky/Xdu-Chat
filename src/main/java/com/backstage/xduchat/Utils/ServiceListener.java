package com.backstage.xduchat.Utils;

import lombok.Getter;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * @Author: 711lxsky
 * @Description:
 */

@Getter
@Component
public class ServiceListener implements ApplicationListener<ApplicationReadyEvent>{

    private long ServiceStartTime;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        this.ServiceStartTime = System.currentTimeMillis();
    }
}
