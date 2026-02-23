package com.edutest.webserver.api.exception;

import com.edutest.api.model.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("Bad request: {} - {}", request.getRequestURI(), ex.getMessage());

        ErrorResponse error = new ErrorResponse();
        error.setError("Bad Request");
        error.setMessage(ex.getMessage());
        error.setTimestamp(OffsetDateTime.now());
        error.setPath(request.getRequestURI());

        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(
            IllegalStateException ex, HttpServletRequest request) {
        log.warn("Conflict: {} - {}", request.getRequestURI(), ex.getMessage());

        ErrorResponse error = new ErrorResponse();
        error.setError("Conflict");
        error.setMessage(ex.getMessage());
        error.setTimestamp(OffsetDateTime.now());
        error.setPath(request.getRequestURI());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse();
        error.setError("Forbidden");
        error.setMessage(ex.getMessage());
        error.setTimestamp(OffsetDateTime.now());
        error.setPath(request.getRequestURI());

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));

        ErrorResponse error = new ErrorResponse();
        error.setError("Validation Error");
        error.setMessage(message);
        error.setTimestamp(OffsetDateTime.now());
        error.setPath(request.getRequestURI());

        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(
            Exception ex, HttpServletRequest request) {
        log.error("Unexpected error: {} - {}", request.getRequestURI(), ex.getMessage(), ex);

        ErrorResponse error = new ErrorResponse();
        error.setError("Internal Server Error");
        error.setMessage("An unexpected error occurred");
        error.setTimestamp(OffsetDateTime.now());
        error.setPath(request.getRequestURI());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
