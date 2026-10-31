package com.ase.wastems;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of the Intelligent Waste Management System REST API.
 *
 * <p>The application exposes a JSON API under {@code /api} that is consumed by the React
 * single-page application stored in {@code frontend/}.
 */
@SpringBootApplication
public class WasteMsApplication {

    public static void main(String[] args) {
        SpringApplication.run(WasteMsApplication.class, args);
    }
}
