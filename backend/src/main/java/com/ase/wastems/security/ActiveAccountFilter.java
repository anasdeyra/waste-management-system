package com.ase.wastems.security;

import com.ase.wastems.domain.AppUser;
import com.ase.wastems.repository.AppUserRepository;
import com.ase.wastems.web.dto.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Rejects requests of sessions whose account was deactivated or removed, so that deactivation
 * takes effect immediately instead of when the session expires.
 */
public class ActiveAccountFilter extends OncePerRequestFilter {

    private final AppUserRepository users;
    private final ObjectMapper mapper;

    public ActiveAccountFilter(AppUserRepository users, ObjectMapper mapper) {
        this.users = users;
        this.mapper = mapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || !a.isAuthenticated() || a instanceof AnonymousAuthenticationToken) {
            chain.doFilter(request, response);
            return;
        }
        boolean ok = users.findByUsername(a.getName()).map(AppUser::isActive).orElse(false);
        if (!ok) {
            SecurityContextHolder.clearContext();
            HttpSession s = request.getSession(false);
            if (s != null) {
                s.invalidate();
            }
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            mapper.writeValue(
                    response.getOutputStream(),
                    ApiError.of(HttpStatus.UNAUTHORIZED, "Authentication required."));
            return;
        }
        chain.doFilter(request, response);
    }
}
