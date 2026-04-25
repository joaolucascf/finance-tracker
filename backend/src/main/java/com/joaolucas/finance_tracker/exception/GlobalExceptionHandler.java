package com.joaolucas.finance_tracker.exception;


import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;


@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler (Exception.class)
    @ResponseStatus (HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ApiError> handleGenericErrors(Exception exception, HttpServletRequest request) {
        ApiError error = ApiErrorFactory.build(HttpStatus.INTERNAL_SERVER_ERROR, request, exception);

        return ResponseEntity.status(error.getStatus()).body(error);
    }

    @ExceptionHandler (NotFoundException.class)
    @ResponseStatus (HttpStatus.NOT_FOUND)
    public ResponseEntity<ApiError> handleNotFoundErrors(NotFoundException exception, HttpServletRequest request) {
        ApiError error = ApiErrorFactory.build(HttpStatus.NOT_FOUND, request, exception);

        return ResponseEntity.status(error.getStatus()).body(error);
    }

    @ExceptionHandler (ForbiddenException.class)
    @ResponseStatus (HttpStatus.FORBIDDEN)
    public ResponseEntity<ApiError> handleForbidden(ForbiddenException exception, HttpServletRequest request) {
        ApiError error = ApiErrorFactory.build(HttpStatus.FORBIDDEN, request, exception);

        return ResponseEntity.status(error.getStatus()).body(error);
    }

    @ExceptionHandler (MethodArgumentNotValidException.class)
    @ResponseStatus (HttpStatus.BAD_REQUEST)
    public ResponseEntity<ApiError> handleValidationErrors(MethodArgumentNotValidException exception,
            HttpServletRequest request) {

        HttpStatus status = HttpStatus.BAD_REQUEST;
        List<FieldErrorDTO> fieldErrors = exception.getFieldErrors()
                .stream()
                .map(error ->
                        new FieldErrorDTO(
                                error.getField(),
                                error.getDefaultMessage()
                        )
                )
                .toList();

        ApiError error = ApiError.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(exception.getMessage())
                .path(request.getRequestURI())
                .errors(fieldErrors)
                .build();

        return ResponseEntity.badRequest().body(error);
    }
}
