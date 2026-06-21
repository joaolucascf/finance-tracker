package com.joaolucas.finance_tracker.openfinance;

import com.joaolucas.finance_tracker.entity.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionData(
        String externalId,
        String description,
        BigDecimal amount,
        TransactionType type,
        LocalDate date
) {}
