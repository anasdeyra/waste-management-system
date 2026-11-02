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
 * the map sample (20), the MN01 import (30) and the routing warm-up (40). With the one-shot
 * option {@code --wastems.demo.reset=true} the demonstration data is deleted first and reloaded
 * (US-33, D-055). It never fails the start-up (NFR-10): any problem is logged and the
 * application keeps running.
 */
@Component
@Profile("dev")
@Order(50)
public class DemoStartupLoader implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(DemoStartupLoader.class);

    private final DemoDataService service;
    private final DemoProperties props;

    public DemoStartupLoader(DemoDataService service, DemoProperties props) {
        this.service = service;
        this.props = props;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!service.isEnabled()) {
            LOG.info("Demo load disabled (wastems.demo.enabled=false)");
            return;
        }
        try {
            if (props.reset()) {
                service.reset(DemoTrigger.STARTUP);
            } else {
                service.load(DemoTrigger.STARTUP);
            }
        } catch (RuntimeException e) {
            LOG.warn("Demo load failed: {}", e.getMessage());
        }
    }
}
