package com.joaolucas.finance_tracker.mapper;


import org.springframework.stereotype.Component;

import com.joaolucas.finance_tracker.dto.auth.RegisterRequestDTO;
import com.joaolucas.finance_tracker.entity.User;


@Component
public class UserMapper {

    public User toEntity(RegisterRequestDTO dto, String encodedPassword) {
        return User.builder()
                .name(dto.getName())
                .email(dto.getEmail())
                .password(encodedPassword)
                .build();
    }
}
