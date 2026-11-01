package com.ase.wastems.service;

/** The caller is authenticated but may not access the resource (HTTP 403). */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
