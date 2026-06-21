package com.joaolucas.finance_tracker.dto.openfinance;

import com.joaolucas.finance_tracker.entity.AccountType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class FinancialAccountResponseDTO {
    private Long id;
    private Long connectionId;
    private String name;
    private AccountType type;
    private BigDecimal currentBalance;
    private String currency;
}
