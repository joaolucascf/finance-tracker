package com.joaolucas.finance_tracker.controller;


import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.joaolucas.finance_tracker.dto.auth.AuthResponseDTO;
import com.joaolucas.finance_tracker.dto.auth.LoginRequestDTO;
import com.joaolucas.finance_tracker.dto.auth.RegisterRequestDTO;
import com.joaolucas.finance_tracker.service.AuthService;

import jakarta.validation.Valid;


@RestController
@RequestMapping ("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping ("/login")
    public AuthResponseDTO login(@RequestBody @Valid LoginRequestDTO dto) {
        return this.authService.doLogin(dto);
    }

    @PostMapping ("/register")
    public AuthResponseDTO register(@RequestBody @Valid RegisterRequestDTO dto) {
        return this.authService.doRegister(dto);
    }
}