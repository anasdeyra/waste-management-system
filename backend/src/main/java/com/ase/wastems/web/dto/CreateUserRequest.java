package com.ase.wastems.web.dto;

import com.ase.wastems.domain.AppRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * Request of an administrator to create an account (72 = BCrypt input limit); the optional
 * agentId links the new AGENT account to an operational agent.
 */
public record CreateUserRequest(
        @NotBlank
                @Size(min = 3, max = 50)
                @Pattern(
                        regexp = "^[A-Za-z0-9._-]+$",
                        message = "may contain only letters, digits, '.', '_' and '-'")
                String username,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotNull AppRole role,
        UUID agentId) {}
