package com.ase.wastems.web;

import com.ase.wastems.domain.AppUser;
import com.ase.wastems.repository.AppUserRepository;
import com.ase.wastems.service.AgentService;
import com.ase.wastems.web.dto.CsrfTokenResponse;
import com.ase.wastems.web.dto.CurrentUserResponse;
import com.ase.wastems.web.dto.LoginRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.Locale;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Session authentication endpoints: CSRF token, login and current user. Logout is a filter. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    /** Header the SPA must use; matches the cookie-based repository configured in SecurityConfig. */
    static final String CSRF_HEADER = "X-XSRF-TOKEN";

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final AppUserRepository users;
    private final AgentService agents;

    public AuthController(
            AuthenticationManager authenticationManager,
            SecurityContextRepository securityContextRepository,
            AppUserRepository users,
            AgentService agents) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.users = users;
        this.agents = agents;
    }

    @GetMapping("/csrf")
    public CsrfTokenResponse csrf(CsrfToken token) {
        return new CsrfTokenResponse(CSRF_HEADER, token.getToken());
    }

    @PostMapping("/login")
    public CurrentUserResponse login(
            @Valid @RequestBody LoginRequest req,
            HttpServletRequest request,
            HttpServletResponse response) {
        String username = req.username().trim().toLowerCase(Locale.ROOT);
        Authentication auth =
                authenticationManager.authenticate(
                        UsernamePasswordAuthenticationToken.unauthenticated(
                                username, req.password()));
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }
        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(auth);
        SecurityContextHolder.setContext(ctx);
        securityContextRepository.saveContext(ctx, request, response);
        return currentUser(users.findByUsername(username).orElseThrow());
    }

    @GetMapping("/me")
    public CurrentUserResponse me(Authentication auth) {
        return currentUser(users.findByUsername(auth.getName()).orElseThrow());
    }

    private CurrentUserResponse currentUser(AppUser user) {
        return CurrentUserResponse.from(user, agents.findByUser(user).orElse(null));
    }
}
