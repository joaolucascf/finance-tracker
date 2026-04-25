package com.joaolucas.finance_tracker.security;


import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.joaolucas.finance_tracker.exception.ApiError;
import com.joaolucas.finance_tracker.exception.ApiErrorFactory;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public JwtAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException)
            throws IOException {

        HttpStatus status = HttpServletResponse.SC_UNAUTHORIZED == 401
                            ? HttpStatus.UNAUTHORIZED
                            : HttpStatus.INTERNAL_SERVER_ERROR;

        ApiError error = ApiErrorFactory.build(
                status,
                request,
                authException
        );

        response.setStatus(status.value());
        response.setContentType("application/json");

        response.getWriter().write(objectMapper.writeValueAsString(error));
    }
}