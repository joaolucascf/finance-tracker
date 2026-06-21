package com.joaolucas.finance_tracker.mapper;

import com.joaolucas.finance_tracker.dto.openfinance.FinancialConnectionResponseDTO;
import com.joaolucas.finance_tracker.entity.FinancialConnection;
import org.springframework.stereotype.Component;

@Component
public class FinancialConnectionMapper {

    public FinancialConnectionResponseDTO toDTO(FinancialConnection connection) {
        return FinancialConnectionResponseDTO.builder()
                .id(connection.getId())
                .institutionName(connection.getInstitutionName())
                .provider(connection.getProvider())
                .status(connection.getStatus())
                .createdAt(connection.getCreatedAt())
                .updatedAt(connection.getUpdatedAt())
                .build();
    }
}
