package com.joaolucas.finance_tracker.service;


import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.joaolucas.finance_tracker.dto.auth.AuthResponseDTO;
import com.joaolucas.finance_tracker.dto.auth.LoginRequestDTO;
import com.joaolucas.finance_tracker.dto.auth.RegisterRequestDTO;
import com.joaolucas.finance_tracker.entity.RefreshToken;
import com.joaolucas.finance_tracker.entity.User;
import com.joaolucas.finance_tracker.mapper.UserMapper;
import com.joaolucas.finance_tracker.repository.UserRepository;
import com.joaolucas.finance_tracker.security.JwtService;


@Service
public class AuthService {

    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;

    public AuthService(JwtService jwtService, PasswordEncoder passwordEncoder, UserMapper userMapper,
            UserRepository userRepository, RefreshTokenService refreshTokenService) {
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
        this.userRepository = userRepository;
        this.refreshTokenService = refreshTokenService;
    }

    public AuthResponseDTO doRegister(RegisterRequestDTO dto) {

        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("Email já cadastrado");
        }

        User user = this.userMapper.toEntity(dto, passwordEncoder.encode(dto.getPassword()));
        userRepository.save(user);

        return buildAuthResponse(user);
    }

    public AuthResponseDTO doLogin(LoginRequestDTO dto) {

        User user = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new RuntimeException("Credenciais inválidas"));

        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new RuntimeException("Credenciais inválidas");
        }

        return buildAuthResponse(user);
    }

    public AuthResponseDTO doRefresh(String refreshTokenValue) {
        User user = refreshTokenService.validateAndRotate(refreshTokenValue);
        return buildAuthResponse(user);
    }

    public void doLogout() {
        User user = getAuthenticatedUser();
        refreshTokenService.revoke(user);
    }

    public Long getAuthenticatedUserId() {
        return (Long) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();
    }

    public User getAuthenticatedUser() {
        Long userId = getAuthenticatedUserId();
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário autenticado não encontrado"));
    }

    private AuthResponseDTO buildAuthResponse(User user) {
        String accessToken = jwtService.generateToken(user.getId());
        RefreshToken refreshToken = refreshTokenService.generate(user);

        return AuthResponseDTO.builder()
                .token(accessToken)
                .refreshToken(refreshToken.getToken())
                .build();
    }
}
