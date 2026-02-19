package com.sourabh.product_service.exception;

import com.sourabh.product_service.common.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ProductNotFoundException ex) {

        return buildError(
                "PRODUCT_NOT_FOUND",
                ex.getMessage(),
                HttpStatus.NOT_FOUND
        );
    }

    @ExceptionHandler(UnauthorizedProductAccessException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedProductAccessException ex) {

        return buildError(
                "UNAUTHORIZED_PRODUCT_ACTION",
                ex.getMessage(),
                HttpStatus.FORBIDDEN
        );
    }

    @ExceptionHandler(ProductStateException.class)
    public ResponseEntity<ErrorResponse> handleState(ProductStateException ex) {

        return buildError(
                "INVALID_PRODUCT_STATE",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {

        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error ->
                        error.getField() + " : " + error.getDefaultMessage())
                .toList();

        ErrorResponse response = ErrorResponse.builder()
                .errorCode("VALIDATION_ERROR")
                .message("Validation failed")
                .details(errors)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {

        log.error("Unexpected error: {}", ex.getMessage());

        return buildError(
                "INTERNAL_SERVER_ERROR",
                "Something went wrong",
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

    private ResponseEntity<ErrorResponse> buildError(
            String code,
            String message,
            HttpStatus status) {

        ErrorResponse response = ErrorResponse.builder()
                .errorCode(code)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(response, status);
    }
}
