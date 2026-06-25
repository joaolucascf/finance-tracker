package com.joaolucas.finance_tracker.mapper;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Component;

import com.joaolucas.finance_tracker.dto.bill.BillResponseDTO;
import com.joaolucas.finance_tracker.dto.transaction.TransactionResponseDTO;
import com.joaolucas.finance_tracker.entity.CreditCardBill;

@Component
public class BillMapper {

    public BillResponseDTO toDTO(CreditCardBill bill, BigDecimal total, List<TransactionResponseDTO> items) {
        return BillResponseDTO.builder()
                .id(bill.getId())
                .name(displayName(bill))
                .total(total)
                .dueDate(bill.getDueDate())
                .status(bill.getStatus().name())
                .items(items)
                .build();
    }

    public String displayName(CreditCardBill bill) {
        String custom = bill.getCustomName();
        if (custom != null && !custom.isBlank()) {
            return custom;
        }
        if (bill.getExternalBillId() == null) {
            return "Fatura atual (em aberto)";
        }
        return "Fatura de Cartão de Crédito #" + bill.getSequence();
    }
}
