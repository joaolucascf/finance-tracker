package com.joaolucas.finance_tracker.dto.transaction;


import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;


@Data
public class TransactionRequestDTO {

    @NotNull
    @Positive
    private Double amount;

    @NotBlank
    private String type; // TODO: melhorar isso.

    private String description;

    @NotNull
    private LocalDate date;

    private Long categoryId;
}