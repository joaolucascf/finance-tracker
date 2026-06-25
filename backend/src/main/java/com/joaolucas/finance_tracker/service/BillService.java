package com.joaolucas.finance_tracker.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.joaolucas.finance_tracker.dto.bill.BillResponseDTO;
import com.joaolucas.finance_tracker.dto.transaction.TransactionResponseDTO;
import com.joaolucas.finance_tracker.entity.CreditCardBill;
import com.joaolucas.finance_tracker.entity.Transaction;
import com.joaolucas.finance_tracker.entity.TransactionType;
import com.joaolucas.finance_tracker.entity.User;
import com.joaolucas.finance_tracker.exception.ForbiddenException;
import com.joaolucas.finance_tracker.exception.NotFoundException;
import com.joaolucas.finance_tracker.mapper.BillMapper;
import com.joaolucas.finance_tracker.mapper.TransactionMapper;
import com.joaolucas.finance_tracker.repository.CreditCardBillRepository;
import com.joaolucas.finance_tracker.repository.TransactionRepository;

@Service
public class BillService {

    private final AuthService authService;
    private final CreditCardBillRepository billRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;
    private final BillMapper billMapper;

    public BillService(AuthService authService,
            CreditCardBillRepository billRepository,
            TransactionRepository transactionRepository,
            TransactionMapper transactionMapper,
            BillMapper billMapper) {
        this.authService = authService;
        this.billRepository = billRepository;
        this.transactionRepository = transactionRepository;
        this.transactionMapper = transactionMapper;
        this.billMapper = billMapper;
    }

    public List<BillResponseDTO> getBillsForPeriod(Long userId, LocalDate start, LocalDate end) {
        return billRepository.findByAccount_UserIdAndDueDateBetweenOrderByDueDateDesc(userId, start, end)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public BillResponseDTO rename(Long id, String name) {
        User user = authService.getAuthenticatedUser();
        CreditCardBill bill = billRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Fatura não encontrada"));
        if (!bill.getAccount().getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Acesso negado");
        }

        String trimmed = name == null ? null : name.trim();
        bill.setCustomName(trimmed == null || trimmed.isBlank() ? null : trimmed);
        bill.setUpdatedAt(LocalDateTime.now());
        billRepository.save(bill);

        return toResponse(bill);
    }

    private BillResponseDTO toResponse(CreditCardBill bill) {
        List<Transaction> transactions = isTransition(bill)
                ? transactionRepository.findBySourceAccount_IdAndBillIsNullAndTypeOrderByDateDescIdAsc(
                        bill.getAccount().getId(), TransactionType.EXPENSE)
                : transactionRepository.findByBillIdOrderByDateDescIdAsc(bill.getId());

        List<TransactionResponseDTO> items = transactions.stream()
                .map(transactionMapper::toDTO)
                .toList();
        return billMapper.toDTO(bill, bill.getTotalAmount(), items);
    }

    /**
     * A transition bill is the open current cycle: it has no provider bill yet (null externalBillId).
     *
     * <p>Both kinds of bill expose {@code totalAmount} as the displayed value, but it means different
     * things and is maintained in different places — always by the sync, never recomputed here:
     * <ul>
     *   <li><b>Closed (provider) bill</b> → {@code totalAmount} is the provider's authoritative figure.
     *       It is <em>not</em> {@code sum(items)}: a real statement includes lines that never arrive as
     *       discrete transactions (interest, IOF, annuity, carried-over balance, FX adjustments), so the
     *       imported items are a possibly-incomplete subset and the provider stays the source of truth.</li>
     *   <li><b>Transition bill</b> → {@code totalAmount} is the sum of the card's still-unbilled EXPENSE
     *       purchases, recomputed on every sync. Here {@code sum(items)} is the truth, because the provider
     *       has no number for the open cycle yet.</li>
     * </ul>
     */
    private boolean isTransition(CreditCardBill bill) {
        return bill.getExternalBillId() == null;
    }
}
