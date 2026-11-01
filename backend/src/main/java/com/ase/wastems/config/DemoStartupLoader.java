package com.ase.wastems.config;

import com.ase.wastems.service.DemoDataService;
import com.ase.wastems.service.DemoTrigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Loads the MN01 demonstration at start-up in the {@code dev} profile, after the accounts (10),
 * the map sample (20), the MN01 import (30) and the routing warm-up (40). It never fails the
 * start-up (NFR-10): any problem is logged and the application keeps running.
 */
@Component
@Profile("dev")
@Order(50)
public class DemoStartupLoader implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(DemoStartupLoader.class);

    private final DemoDataService service;

    public DemoStartupLoader(DemoDataService service) {
        this.service = service;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!service.isEnabled()) {
            LOG.info("Demo load disabled (wastems.demo.enabled=false)");
            return;
        }
        try {
            service.load(DemoTrigger.STARTUP);
        } catch (RuntimeException e) {
            LOG.warn("Demo load failed: {}", e.getMessage());
        }
    }
}
