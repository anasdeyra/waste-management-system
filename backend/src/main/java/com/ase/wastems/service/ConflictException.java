package com.ase.wastems.service;

/** Uniqueness or state conflict (HTTP 409). */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
