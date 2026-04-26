package com.joaolucas.finance_tracker.dto.transaction;


import java.math.BigDecimal;
import java.time.LocalDate;

import com.joaolucas.finance_tracker.entity.TransactionType;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;


@Data
public class TransactionRequestDTO {

    @NotNull
    @Positive
    private BigDecimal amount;

    @NotNull
    private TransactionType type;

    private String description;

    @NotNull
    private LocalDate date;

    private Long categoryId;
}