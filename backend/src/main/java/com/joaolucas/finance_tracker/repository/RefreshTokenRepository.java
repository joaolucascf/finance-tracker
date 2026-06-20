package com.joaolucas.finance_tracker.repository;

import com.joaolucas.finance_tracker.entity.RefreshToken;
import com.joaolucas.finance_tracker.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    void deleteByUser(User user);
}
