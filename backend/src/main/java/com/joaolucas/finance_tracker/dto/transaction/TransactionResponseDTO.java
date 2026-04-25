package com.joaolucas.finance_tracker.dto.transaction;


import java.time.LocalDate;

import com.joaolucas.finance_tracker.dto.category.CategoryResponseDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TransactionResponseDTO {

    private Long id;
    private Double amount;
    private String type;
    private String description;
    private LocalDate date;
    private CategoryResponseDTO category;
}