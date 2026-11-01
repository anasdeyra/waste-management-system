package com.ase.wastems.web.dto;

/** CSRF token and the name of the header that must carry it on unsafe requests. */
public record CsrfTokenResponse(String headerName, String token) {}
