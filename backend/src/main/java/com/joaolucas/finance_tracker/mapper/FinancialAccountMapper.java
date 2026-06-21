package com.joaolucas.finance_tracker.mapper;

import com.joaolucas.finance_tracker.dto.openfinance.FinancialAccountResponseDTO;
import com.joaolucas.finance_tracker.entity.FinancialAccount;
import org.springframework.stereotype.Component;

@Component
public class FinancialAccountMapper {

    public FinancialAccountResponseDTO toDTO(FinancialAccount account) {
        return FinancialAccountResponseDTO.builder()
                .id(account.getId())
                .connectionId(account.getConnection().getId())
                .name(account.getName())
                .type(account.getType())
                .currentBalance(account.getCurrentBalance())
                .currency(account.getCurrency())
                .build();
    }
}
