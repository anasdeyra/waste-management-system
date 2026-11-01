package com.ase.wastems.config;

import com.ase.wastems.routing.RoadRouter;
import com.ase.wastems.routing.RoutingUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Builds or loads the road graph at start-up in the dev profile. A failure only logs a warning:
 * the rest of the application keeps working (NFR-10).
 */
@Component
@Profile("dev")
@Order(40)
public class RoutingWarmup implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(RoutingWarmup.class);

    private final RoadRouter router;

    public RoutingWarmup(RoadRouter router) {
        this.router = router;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            LOG.info("Routing ready: {} / {}", router.roadGraphVersion(), router.routingEngine());
        } catch (RoutingUnavailableException e) {
            LOG.warn("Road routing unavailable: {}", e.getMessage());
        }
    }
}
