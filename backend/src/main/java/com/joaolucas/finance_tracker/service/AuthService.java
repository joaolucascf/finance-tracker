package com.joaolucas.finance_tracker.service;


import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.joaolucas.finance_tracker.dto.auth.AuthResponseDTO;
import com.joaolucas.finance_tracker.dto.auth.LoginRequestDTO;
import com.joaolucas.finance_tracker.dto.auth.RegisterRequestDTO;
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

    public AuthService(JwtService jwtService, PasswordEncoder passwordEncoder, UserMapper userMapper,
            UserRepository userRepository) {
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
        this.userRepository = userRepository;
    }

    public AuthResponseDTO doRegister(RegisterRequestDTO dto) {

        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("Email já cadastrado");
        }

        User user = this.userMapper.toEntity(dto, passwordEncoder.encode(dto.getPassword()));

        userRepository.save(user);

        String token = jwtService.generateToken(user.getId());

        return AuthResponseDTO.builder()
                .token(token)
                .build();
    }

    public AuthResponseDTO doLogin(LoginRequestDTO dto) {

        User user = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new RuntimeException("Credenciais inválidas"));

        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new RuntimeException("Credenciais inválidas");
        }

        String token = jwtService.generateToken(user.getId());

        return AuthResponseDTO.builder()
                .token(token)
                .build();
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
}