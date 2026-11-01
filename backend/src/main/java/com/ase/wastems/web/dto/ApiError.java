package com.ase.wastems.web.dto;

import java.util.List;
import org.springframework.http.HttpStatus;

/** Uniform error body of the REST API; {@code issues} lists blocking findings of a refused confirmation. */
public record ApiError(int status, String error, String message, List<FieldErrorDetail> fieldErrors,
        List<TourIssueResponse> issues, List<CsvRowErrorResponse> rowErrors) {

    public ApiError(int status, String error, String message, List<FieldErrorDetail> fieldErrors) {
        this(status, error, message, fieldErrors, null, null);
    }

    public ApiError(int status, String error, String message, List<FieldErrorDetail> fieldErrors,
            List<TourIssueResponse> issues) {
        this(status, error, message, fieldErrors, issues, null);
    }

    public static ApiError of(HttpStatus status, String message) {
        return new ApiError(status.value(), status.getReasonPhrase(), message, null);
    }
}
