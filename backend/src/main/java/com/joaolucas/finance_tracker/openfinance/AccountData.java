package com.joaolucas.finance_tracker.openfinance;

import com.joaolucas.finance_tracker.entity.AccountType;

import java.math.BigDecimal;

public record AccountData(
        String externalId,
        String name,
        AccountType type,
        BigDecimal balance,
        String currency
) {}
