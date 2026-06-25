package com.joaolucas.finance_tracker.dto.bill;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.joaolucas.finance_tracker.dto.transaction.TransactionResponseDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BillResponseDTO {

    private Long id;
    private String name;
    private BigDecimal total;
    private LocalDate dueDate;
    private String status;
    private List<TransactionResponseDTO> items;
}
