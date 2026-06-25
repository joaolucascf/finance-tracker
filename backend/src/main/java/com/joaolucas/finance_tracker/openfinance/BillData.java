package com.joaolucas.finance_tracker.openfinance;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BillData(
        String externalId,
        LocalDate dueDate,
        BigDecimal totalAmount
) {}
