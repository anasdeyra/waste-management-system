package com.ase.wastems.web.dto;

import java.util.List;
import org.springframework.http.HttpStatus;

/** Uniform error body of the REST API. */
public record ApiError(int status, String error, String message, List<FieldErrorDetail> fieldErrors) {

    public static ApiError of(HttpStatus status, String message) {
        return new ApiError(status.value(), status.getReasonPhrase(), message, null);
    }
}
