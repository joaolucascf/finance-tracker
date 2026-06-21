package com.joaolucas.finance_tracker.dto.openfinance;

import com.joaolucas.finance_tracker.entity.ConnectionStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class FinancialConnectionResponseDTO {
    private Long id;
    private String institutionName;
    private String provider;
    private ConnectionStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
