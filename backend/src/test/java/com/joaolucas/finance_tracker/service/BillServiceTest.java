package com.joaolucas.finance_tracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.joaolucas.finance_tracker.dto.bill.BillResponseDTO;
import com.joaolucas.finance_tracker.entity.BillStatus;
import com.joaolucas.finance_tracker.entity.CreditCardBill;
import com.joaolucas.finance_tracker.entity.FinancialAccount;
import com.joaolucas.finance_tracker.entity.User;
import com.joaolucas.finance_tracker.exception.ForbiddenException;
import com.joaolucas.finance_tracker.exception.NotFoundException;
import com.joaolucas.finance_tracker.entity.TransactionType;
import com.joaolucas.finance_tracker.mapper.BillMapper;
import com.joaolucas.finance_tracker.mapper.TransactionMapper;
import com.joaolucas.finance_tracker.repository.CreditCardBillRepository;
import com.joaolucas.finance_tracker.repository.TransactionRepository;

@ExtendWith(MockitoExtension.class)
class BillServiceTest {

    @Mock
    private AuthService authService;
    @Mock
    private CreditCardBillRepository billRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private TransactionMapper transactionMapper;
    @Mock
    private BillMapper billMapper;

    @InjectMocks
    private BillService billService;

    private User user;

    @BeforeEach
    void setup() {
        user = User.builder().id(1L).build();
    }

    /** Closed (provider) bill: has an externalBillId. */
    private CreditCardBill bill(Long id, BillStatus status, BigDecimal total) {
        FinancialAccount account = FinancialAccount.builder().id(99L).user(user).build();
        return CreditCardBill.builder()
                .id(id).account(account).status(status)
                .externalBillId("ext-" + id)
                .totalAmount(total).dueDate(LocalDate.of(2026, 6, 26))
                .sequence(1).build();
    }

    // ===== getBillsForPeriod / total =====
    @Test
    void getBillsForPeriod$closedBillTotalIsProviderTotal() {
        CreditCardBill bill = bill(11L, BillStatus.CLOSED, new BigDecimal("1000.00"));

        when(billRepository.findByAccount_UserIdAndDueDateBetweenOrderByDueDateDesc(eq(1L), any(), any()))
                .thenReturn(List.of(bill));
        when(transactionRepository.findByBillIdOrderByDateDescIdAsc(11L)).thenReturn(List.of());

        billService.getBillsForPeriod(1L, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));

        ArgumentCaptor<BigDecimal> totalCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        verify(billMapper).toDTO(eq(bill), totalCaptor.capture(), any());
        assertEquals(0, new BigDecimal("1000.00").compareTo(totalCaptor.getValue()));
    }

    @Test
    void getBillsForPeriod$transitionBillUsesStoredTotalAndOrphanItems() {
        CreditCardBill transition = CreditCardBill.builder()
                .id(20L)
                .account(FinancialAccount.builder().id(99L).user(user).build())
                .status(BillStatus.OPEN)
                .totalAmount(new BigDecimal("3998.37"))
                .dueDate(LocalDate.of(2026, 7, 9))
                .build(); // no externalBillId -> transition bill

        when(billRepository.findByAccount_UserIdAndDueDateBetweenOrderByDueDateDesc(eq(1L), any(), any()))
                .thenReturn(List.of(transition));
        when(transactionRepository.findBySourceAccount_IdAndBillIsNullAndTypeOrderByDateDescIdAsc(99L, TransactionType.EXPENSE))
                .thenReturn(List.of());

        billService.getBillsForPeriod(1L, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31));

        ArgumentCaptor<BigDecimal> totalCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        verify(billMapper).toDTO(eq(transition), totalCaptor.capture(), any());
        assertEquals(0, new BigDecimal("3998.37").compareTo(totalCaptor.getValue()));
        verify(transactionRepository)
                .findBySourceAccount_IdAndBillIsNullAndTypeOrderByDateDescIdAsc(99L, TransactionType.EXPENSE);
    }

    // ===== rename =====
    @Test
    void rename$setsCustomNameForOwner() {
        CreditCardBill bill = bill(10L, BillStatus.CLOSED, new BigDecimal("1000.00"));
        when(authService.getAuthenticatedUser()).thenReturn(user);
        when(billRepository.findById(10L)).thenReturn(Optional.of(bill));
        when(transactionRepository.findByBillIdOrderByDateDescIdAsc(10L)).thenReturn(List.of());
        when(billMapper.toDTO(any(), any(), any())).thenReturn(new BillResponseDTO());

        billService.rename(10L, "  Fatura do Nubank  ");

        assertEquals("Fatura do Nubank", bill.getCustomName());
        verify(billRepository).save(bill);
    }

    @Test
    void rename$blankNameClearsCustomName() {
        CreditCardBill bill = bill(10L, BillStatus.CLOSED, new BigDecimal("1000.00"));
        bill.setCustomName("Old");
        when(authService.getAuthenticatedUser()).thenReturn(user);
        when(billRepository.findById(10L)).thenReturn(Optional.of(bill));
        when(transactionRepository.findByBillIdOrderByDateDescIdAsc(10L)).thenReturn(List.of());
        when(billMapper.toDTO(any(), any(), any())).thenReturn(new BillResponseDTO());

        billService.rename(10L, "   ");

        assertNull(bill.getCustomName());
    }

    @Test
    void rename$throwsForbiddenWhenNotOwner() {
        CreditCardBill bill = bill(10L, BillStatus.CLOSED, new BigDecimal("1000.00"));
        bill.getAccount().setUser(User.builder().id(2L).build());
        when(authService.getAuthenticatedUser()).thenReturn(user);
        when(billRepository.findById(10L)).thenReturn(Optional.of(bill));

        assertThrows(ForbiddenException.class, () -> billService.rename(10L, "x"));
        verify(billRepository, never()).save(any());
    }

    @Test
    void rename$throwsNotFoundWhenMissing() {
        when(authService.getAuthenticatedUser()).thenReturn(user);
        when(billRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> billService.rename(99L, "x"));
    }
}
