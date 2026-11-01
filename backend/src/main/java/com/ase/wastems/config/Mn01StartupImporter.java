package com.ase.wastems.config;

import com.ase.wastems.service.ImportTrigger;
import com.ase.wastems.service.Mn01ImportReport;
import com.ase.wastems.service.Mn01ImportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Runs the idempotent MN01 import at start-up in the {@code dev} profile, after the accounts
 * (order 10) and the map sample (order 20), so the sample rows are absorbed, not duplicated.
 */
@Component
@Profile("dev")
@Order(30)
public class Mn01StartupImporter implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(Mn01StartupImporter.class);

    private final Mn01ImportService service;

    public Mn01StartupImporter(Mn01ImportService service) {
        this.service = service;
    }

    @Override
    public void run(ApplicationArguments args) {
        Mn01ImportReport r = service.importAll(ImportTrigger.STARTUP);
        LOG.info(
                "MN01 import (STARTUP): baskets created={}, existing={}, updated={}, skipped={},"
                        + " snapshot={}; depots created={}, existing={}",
                r.baskets().created(),
                r.baskets().existing(),
                r.baskets().updated(),
                r.baskets().skipped(),
                r.baskets().snapshotId(),
                r.depots().created(),
                r.depots().existing());
    }
}
