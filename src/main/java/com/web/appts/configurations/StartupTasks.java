package com.web.appts.configurations;

import com.web.appts.services.OrderService;
import com.web.appts.services.OrderStartUpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

@Configuration
public class StartupTasks {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderStartUpService orderStartUpService;

    private boolean isAppStarted = false;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        isAppStarted = true;
        orderService.markExpired();
        orderService.checkOrderExistence();
        orderStartUpService.verifyUpdateAddSmeSpuDots();
        System.out.println("Application started successfully. Refresh will occur in 30 minutes.");
    }

}
