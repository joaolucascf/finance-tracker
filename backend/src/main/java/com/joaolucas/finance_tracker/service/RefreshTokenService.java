package com.joaolucas.finance_tracker.service;

import com.joaolucas.finance_tracker.entity.RefreshToken;
import com.joaolucas.finance_tracker.entity.User;
import com.joaolucas.finance_tracker.repository.RefreshTokenRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private static final int EXPIRY_DAYS = 7;

    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Transactional
    public RefreshToken generate(User user) {
        refreshTokenRepository.deleteByUser(user);

        RefreshToken token = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiresAt(LocalDateTime.now().plusDays(EXPIRY_DAYS))
                .build();

        return refreshTokenRepository.save(token);
    }

    @Transactional
    public User validateAndRotate(String tokenValue) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new BadCredentialsException("Refresh token inválido"));

        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(refreshToken);
            throw new BadCredentialsException("Refresh token expirado");
        }

        User user = refreshToken.getUser();
        refreshTokenRepository.delete(refreshToken);
        return user;
    }

    @Transactional
    public void revoke(User user) {
        refreshTokenRepository.deleteByUser(user);
    }
}
