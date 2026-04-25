package com.joaolucas.finance_tracker.dto.auth;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class AuthResponseDTO {
    private String token;
}