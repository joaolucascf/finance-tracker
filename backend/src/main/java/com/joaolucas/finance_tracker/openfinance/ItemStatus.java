package com.joaolucas.finance_tracker.openfinance;

public record ItemStatus(
        String externalId,
        String institutionName,
        boolean active
) {}
