package com.joaolucas.finance_tracker.controller;


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.joaolucas.finance_tracker.dto.auth.AuthResponseDTO;
import com.joaolucas.finance_tracker.dto.auth.LoginRequestDTO;
import com.joaolucas.finance_tracker.dto.auth.RefreshRequestDTO;
import com.joaolucas.finance_tracker.dto.auth.RegisterRequestDTO;
import com.joaolucas.finance_tracker.service.AuthService;

import jakarta.validation.Valid;


@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public AuthResponseDTO login(@RequestBody @Valid LoginRequestDTO dto) {
        return this.authService.doLogin(dto);
    }

    @PostMapping("/register")
    public AuthResponseDTO register(@RequestBody @Valid RegisterRequestDTO dto) {
        return this.authService.doRegister(dto);
    }

    @PostMapping("/refresh")
    public AuthResponseDTO refresh(@RequestBody @Valid RefreshRequestDTO dto) {
        return this.authService.doRefresh(dto.getRefreshToken());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout() {
        this.authService.doLogout();
    }
}
