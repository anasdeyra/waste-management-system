package com.ase.wastems.config;

import com.ase.wastems.service.DemandProfile;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Seeded MN01 evaluation experiment (D-016, D-022, D-050). Only the vehicle capacity is used by
 * the services; the other fields configure the headless runner (profile experiment).
 */
@ConfigurationProperties(prefix = "wastems.experiment")
public record ExperimentProperties(
        BigDecimal vehicleCapacityKg,
        List<DemandProfile> profiles,
        List<Long> seeds,
        List<Integer> sizes,
        Integer repetitions,
        String label,
        String outputDir) {}
