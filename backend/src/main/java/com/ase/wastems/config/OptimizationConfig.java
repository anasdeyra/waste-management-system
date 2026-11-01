package com.ase.wastems.config;

import com.ase.wastems.optimization.TourOptimizationModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Exposes the annotation-free optimization core as a bean. */
@Configuration
public class OptimizationConfig {

    @Bean
    TourOptimizationModule tourOptimizationModule() {
        return new TourOptimizationModule();
    }
}
