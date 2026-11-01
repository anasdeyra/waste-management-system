package com.ase.wastems.service;

/** A cross-field rule of a request body is violated (HTTP 400, same body as bean validation). */
public class InvalidRequestException extends RuntimeException {

    private final String field;

    public InvalidRequestException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
