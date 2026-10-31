package com.ase.wastems.web.dto;

import java.util.List;

/** Profile of the authenticated user. */
public record UserProfileResponse(Long id, String username, String fullName, List<String> roles) {}
