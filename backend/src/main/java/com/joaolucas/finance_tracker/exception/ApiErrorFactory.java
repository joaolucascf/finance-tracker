package com.joaolucas.finance_tracker.exception;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;

import jakarta.servlet.http.HttpServletRequest;

public class ApiErrorFactory {

    public static ApiError build(HttpStatus status,
            HttpServletRequest request,
            Exception exception) {

        return ApiError.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(exception.getMessage())
                .path(request.getRequestURI())
                .build();
    }
}