package com.ase.wastems.web.dto;

/** Successful authentication result: a bearer token and the matching profile. */
public record LoginResponse(String token, UserProfileResponse user) {}
