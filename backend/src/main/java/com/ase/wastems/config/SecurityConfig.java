package com.ase.wastems.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration of the REST API.
 *
 * <p>Authentication and role-based authorization (FR-01, FR-02) are implemented in the
 * authentication sprint. Until then every endpoint is open and only the H2 console keeps its
 * frame options header, which is required by the console itself.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(registry -> registry.anyRequest().permitAll())
                .headers(
                        headers ->
                                headers.frameOptions(
                                        frame ->
                                                frame.sameOrigin()));

        return http.build();
    }
}
