package com.ase.wastems.config;

import com.ase.wastems.repository.AppUserRepository;
import com.ase.wastems.security.ActiveAccountFilter;
import com.ase.wastems.security.JsonAccessDeniedHandler;
import com.ase.wastems.security.JsonAuthenticationEntryPoint;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.DispatcherType;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.RequestCacheConfigurer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

/**
 * Security configuration of the REST API: HttpSession authentication, cookie-based CSRF
 * protection (D-038) and role-based URL authorization with ADMIN inheriting PLANNER (D-026).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.withDefaultRolePrefix().role("ADMIN").implies("PLANNER").build();
    }

    @Bean
    public PathPatternRequestMatcher.Builder pathPatternRequestMatcherBuilder() {
        return PathPatternRequestMatcher.withDefaults();
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            AppUserRepository users,
            ObjectMapper om,
            SecurityContextRepository repo)
            throws Exception {
        CookieCsrfTokenRepository csrfRepo = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfRepo.setCookiePath("/");
        http.csrf(
                        c ->
                                c.csrfTokenRepository(csrfRepo)
                                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                                        .ignoringRequestMatchers(PathRequest.toH2Console()))
                .securityContext(c -> c.securityContextRepository(repo))
                .requestCache(RequestCacheConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(
                        l ->
                                l.logoutUrl("/api/auth/logout")
                                        .logoutSuccessHandler(
                                                new HttpStatusReturningLogoutSuccessHandler(
                                                        HttpStatus.NO_CONTENT))
                                        .invalidateHttpSession(true)
                                        .clearAuthentication(true)
                                        .deleteCookies("JSESSIONID"))
                .exceptionHandling(
                        e ->
                                e.authenticationEntryPoint(new JsonAuthenticationEntryPoint(om))
                                        .accessDeniedHandler(new JsonAccessDeniedHandler(om)))
                .addFilterBefore(new ActiveAccountFilter(users, om), AuthorizationFilter.class)
                .authorizeHttpRequests(
                        r ->
                                r.dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.FORWARD)
                                        .permitAll()
                                        .requestMatchers("/error")
                                        .permitAll()
                                        .requestMatchers(PathRequest.toH2Console())
                                        .permitAll()
                                        .requestMatchers(HttpMethod.GET, "/api/auth/csrf")
                                        .permitAll()
                                        .requestMatchers(HttpMethod.POST, "/api/auth/login")
                                        .permitAll()
                                        .requestMatchers("/api/users", "/api/users/**")
                                        .hasRole("ADMIN")
                                        .requestMatchers(
                                                HttpMethod.GET,
                                                "/api/waste-categories",
                                                "/api/waste-categories/**")
                                        .authenticated()
                                        .requestMatchers(
                                                "/api/waste-categories", "/api/waste-categories/**")
                                        .hasRole("ADMIN")
                                        .anyRequest()
                                        .authenticated())
                .headers(h -> h.frameOptions(f -> f.sameOrigin()));
        return http.build();
    }
}
