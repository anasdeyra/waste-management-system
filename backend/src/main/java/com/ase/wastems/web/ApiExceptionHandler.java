package com.ase.wastems.web;

import com.ase.wastems.service.BusinessRuleException;
import com.ase.wastems.service.ConflictException;
import com.ase.wastems.routing.RoutingUnavailableException;
import com.ase.wastems.service.NotFoundException;
import com.ase.wastems.web.dto.ApiError;
import com.ase.wastems.web.dto.FieldErrorDetail;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
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

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> notFound(NotFoundException ex) {
        return body(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> conflict(ConflictException ex) {
        return body(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiError> businessRule(BusinessRuleException ex) {
        return body(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> integrity(DataIntegrityViolationException ex) {
        return body(HttpStatus.CONFLICT, "The request conflicts with existing data.");
    }

    @ExceptionHandler(RoutingUnavailableException.class)
    public ResponseEntity<ApiError> routingUnavailable(RoutingUnavailableException ex) {
        return body(
                HttpStatus.SERVICE_UNAVAILABLE, "Road routing is unavailable: " + ex.getMessage());
    }
}
