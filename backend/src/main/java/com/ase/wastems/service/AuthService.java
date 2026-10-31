package com.ase.wastems.service;

import com.ase.wastems.security.AuthenticatedUser;
import com.ase.wastems.security.AuthenticatedUserService;
import com.ase.wastems.security.TokenService;
import com.ase.wastems.web.dto.LoginRequest;
import com.ase.wastems.web.dto.LoginResponse;
import com.ase.wastems.web.dto.UserProfileResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Handles authentication (FR-01). */
@Service
public class AuthService {

    private final AuthenticatedUserService userService;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public AuthService(
            AuthenticatedUserService userService,
            PasswordEncoder passwordEncoder,
            TokenService tokenService) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        AuthenticatedUser user =
                userService
                        .findByUsername(request.username())
                        .filter(candidate -> passwordEncoder.matches(request.password(), candidate.getPassword()))
                        .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

        String token = tokenService.issue(user.getUsername());
        return new LoginResponse(token, toProfile(user));
    }

    public void logout(String token) {
        tokenService.revoke(token);
    }

    public UserProfileResponse toProfile(AuthenticatedUser user) {
        return new UserProfileResponse(user.getId(), user.getUsername(), user.getFullName(), user.getRoles());
    }
}
