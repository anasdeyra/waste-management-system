package com.ase.wastems.web;

import com.ase.wastems.web.dto.ApiError;
import com.ase.wastems.web.dto.FieldErrorDetail;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Maps exceptions to the uniform {@link ApiError} body and the status conventions of D-042. */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static ResponseEntity<ApiError> body(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(ApiError.of(status, message));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> authentication(AuthenticationException ex) {
        return body(HttpStatus.UNAUTHORIZED, "Invalid username or password.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex) {
        List<FieldErrorDetail> errors =
                ex.getBindingResult().getFieldErrors().stream()
                        .map(f -> new FieldErrorDetail(f.getField(), f.getDefaultMessage()))
                        .toList();
        HttpStatus s = HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(s)
                .body(new ApiError(s.value(), s.getReasonPhrase(), "Validation failed.", errors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> unreadable(HttpMessageNotReadableException ex) {
        return body(HttpStatus.BAD_REQUEST, "Malformed request body.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> typeMismatch(MethodArgumentTypeMismatchException ex) {
        return body(HttpStatus.BAD_REQUEST, "Invalid value for parameter '" + ex.getName() + "'.");
    }
}
