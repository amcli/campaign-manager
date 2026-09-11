package com.dnd.campaignmanager.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> notFound(ResourceNotFoundException e) {
        return respond(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiError> forbidden(ForbiddenException e) {
        return respond(HttpStatus.FORBIDDEN, e.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> conflict(ConflictException e) {
        return respond(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> badCredentials(BadCredentialsException e) {
        return respond(HttpStatus.UNAUTHORIZED, "Invalid username or password");
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ApiError> invalidRequest(InvalidRequestException e) {
        return ResponseEntity.badRequest()
                .body(ApiError.of(HttpStatus.BAD_REQUEST.value(), e.getMessage(), e.getFieldErrors()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> unreadable(HttpMessageNotReadableException e) {
        return respond(HttpStatus.BAD_REQUEST, "Request body is malformed");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> typeMismatch(MethodArgumentTypeMismatchException e) {
        return respond(HttpStatus.BAD_REQUEST, "Invalid value for " + e.getName());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validationFailed(MethodArgumentNotValidException e) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError error : e.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        return ResponseEntity.badRequest()
                .body(ApiError.of(HttpStatus.BAD_REQUEST.value(), "Validation failed", fieldErrors));
    }

    private ResponseEntity<ApiError> respond(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(ApiError.of(status.value(), message));
    }
}
