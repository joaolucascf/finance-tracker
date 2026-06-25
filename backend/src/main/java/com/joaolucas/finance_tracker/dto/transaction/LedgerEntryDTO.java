package com.joaolucas.finance_tracker.dto.transaction;

import java.time.LocalDate;

import com.joaolucas.finance_tracker.dto.bill.BillResponseDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A single row in the dashboard ledger: either a standalone transaction or an aggregated credit-card bill.
 * {@code date} is the sort key (transaction date, or the bill's due date).
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LedgerEntryDTO {

    public enum Type { TRANSACTION, BILL }

    private Type type;
    private LocalDate date;
    private TransactionResponseDTO transaction;
    private BillResponseDTO bill;

    public static LedgerEntryDTO of(TransactionResponseDTO transaction) {
        return LedgerEntryDTO.builder()
                .type(Type.TRANSACTION)
                .date(transaction.getDate())
                .transaction(transaction)
                .build();
    }

    public static LedgerEntryDTO of(BillResponseDTO bill) {
        return LedgerEntryDTO.builder()
                .type(Type.BILL)
                .date(bill.getDueDate())
                .bill(bill)
                .build();
    }
}
