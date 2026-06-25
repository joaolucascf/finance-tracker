package com.joaolucas.finance_tracker.service;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.joaolucas.finance_tracker.dto.transaction.LedgerEntryDTO;
import com.joaolucas.finance_tracker.dto.transaction.TransactionRequestDTO;
import com.joaolucas.finance_tracker.dto.transaction.TransactionResponseDTO;
import com.joaolucas.finance_tracker.entity.Category;
import com.joaolucas.finance_tracker.entity.CreditCardBill;
import com.joaolucas.finance_tracker.entity.Transaction;
import com.joaolucas.finance_tracker.entity.TransactionType;
import com.joaolucas.finance_tracker.entity.User;
import com.joaolucas.finance_tracker.exception.ConflictException;
import com.joaolucas.finance_tracker.exception.ForbiddenException;
import com.joaolucas.finance_tracker.exception.NotFoundException;
import com.joaolucas.finance_tracker.mapper.TransactionMapper;
import com.joaolucas.finance_tracker.repository.TransactionRepository;

import jakarta.persistence.PersistenceException;


@ExtendWith (MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private AuthService authService;
    @Mock
    private CategoryService categoryService;
    @Mock
    private TransactionMapper transactionMapper;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private BillService billService;

    @InjectMocks
    private TransactionService transactionService;

    private User user;
    private Category category;
    private Transaction transaction;
    private TransactionRequestDTO requestDTO;
    private TransactionResponseDTO responseDTO;

    @BeforeEach
    void setup() {
        user = user(1L);
        category = category(10L);

        requestDTO = new TransactionRequestDTO();
        requestDTO.setCategoryId(category.getId());

        transaction = new Transaction();
        responseDTO = new TransactionResponseDTO();
    }

    // ===== CREATE =====
    @Test
    void create$shouldCreateTransactionSuccessfully() {
        // given
        mockDefaultFlow();

        when(transactionRepository.save(transaction))
                .thenReturn(transaction);

        when(transactionMapper.toDTO(transaction))
                .thenReturn(responseDTO);

        // when
        TransactionResponseDTO result = transactionService.create(requestDTO);

        // then
        assertNotNull(result);
        assertEquals(responseDTO, result);

        verify(authService).getAuthenticatedUser();
        verify(categoryService).getIfAvailableForUser(10L, 1L);
        verify(transactionMapper).toEntity(requestDTO, user, category);
        verify(transactionRepository).save(transaction);
        verify(transactionMapper).toDTO(transaction);
    }

    @Test
    void create$shouldThrowExceptionWhenCategoryNotFound() {
        // given
        when(authService.getAuthenticatedUser()).thenReturn(user);

        when(categoryService.getIfAvailableForUser(category.getId(), user.getId()))
                .thenThrow(new ForbiddenException("Categoria inválida para este usuário"));

        // when + then
        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> transactionService.create(requestDTO)
        );

        assertEquals("Categoria inválida para este usuário", exception.getMessage());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void create$shouldThrowExceptionWhenUserNotAuthenticated() {
        // given
        when(authService.getAuthenticatedUser()).thenThrow(new RuntimeException("Usuário autenticado não encontrado"));

        // when + then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> transactionService.create(requestDTO)
        );

        assertEquals("Usuário autenticado não encontrado", exception.getMessage());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void create$shouldThrowExceptionWhenRepositorySaveFails() {
        // given
        mockDefaultFlow();

        when(transactionRepository.save(transaction))
                .thenThrow(new PersistenceException("Não foi possível criar a transação"));

        // when + then
        PersistenceException exception = assertThrows(
                PersistenceException.class,
                () -> transactionService.create(requestDTO)
        );

        assertEquals("Não foi possível criar a transação", exception.getMessage());
        verify(transactionMapper, never()).toDTO(any());
    }

    // ===== GET BY USER =====
    @Test
    void getByAuthenticatedUser$shouldReturnMappedTransactionsWhenUserHasTransactions() {
        // given
        when(authService.getAuthenticatedUser()).thenReturn(user);

        Transaction transaction = new Transaction();
        TransactionResponseDTO dto = new TransactionResponseDTO();

        List<Transaction> mockReturn = List.of(transaction);

        when(transactionRepository.findVisibleLedgerEntries(eq(user.getId()), any(), any(), any(), any()))
                .thenReturn(mockReturn);

        when(transactionMapper.toDTO(transaction))
                .thenReturn(dto);

        // when
        List<LedgerEntryDTO> result = this.transactionService.getByAuthenticatedUser(null, null);

        // then
        assertEquals(1, result.size());
        assertEquals(dto, result.get(0).getTransaction());
        verify(transactionMapper).toDTO(any());
    }

    @Test
    void getByAuthenticatedUser$shouldReturnEmptyListWhenUserHasNoTransactions() {
        // given
        when(authService.getAuthenticatedUser()).thenReturn(user);

        when(transactionRepository.findVisibleLedgerEntries(eq(user.getId()), any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        // when
        List<LedgerEntryDTO> result = this.transactionService.getByAuthenticatedUser(null, null);

        // then
        assertTrue(result.isEmpty());
        verify(transactionMapper, never()).toDTO(any());
    }

    @Test
    void getByAuthenticatedUser$shouldThrowExceptionWhenUserNotAuthenticated() {
        // given
        when(authService.getAuthenticatedUser()).thenThrow(new RuntimeException("Usuário autenticado não encontrado"));

        // when + then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> transactionService.getByAuthenticatedUser(null, null)
        );

        assertEquals("Usuário autenticado não encontrado", exception.getMessage());
        verify(transactionRepository, never())
                .findVisibleLedgerEntries(anyLong(), any(), any(), any(), any());
        verify(transactionMapper, never()).toDTO(any());
    }

    // ===== UPDATE =====
    @Test
    void update$shouldUpdateAllFieldsWhenNotImported() {
        // given
        Transaction existing = Transaction.builder()
                .id(5L).user(user).imported(false)
                .amount(BigDecimal.TEN).type(TransactionType.EXPENSE)
                .date(LocalDate.of(2026, 1, 1)).description("old")
                .category(category).build();

        Category newCategory = category(20L);

        TransactionRequestDTO dto = new TransactionRequestDTO();
        dto.setAmount(BigDecimal.valueOf(99));
        dto.setType(TransactionType.INCOME);
        dto.setDate(LocalDate.of(2026, 6, 10));
        dto.setDescription("new");
        dto.setCategoryId(20L);

        when(authService.getAuthenticatedUser()).thenReturn(user);
        when(transactionRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(categoryService.getIfAvailableForUser(20L, 1L)).thenReturn(newCategory);
        when(transactionRepository.save(existing)).thenReturn(existing);
        when(transactionMapper.toDTO(existing)).thenReturn(responseDTO);

        // when
        TransactionResponseDTO result = transactionService.update(5L, dto);

        // then
        assertEquals(responseDTO, result);
        assertEquals(BigDecimal.valueOf(99), existing.getAmount());
        assertEquals(TransactionType.INCOME, existing.getType());
        assertEquals(LocalDate.of(2026, 6, 10), existing.getDate());
        assertEquals("new", existing.getDescription());
        assertEquals(newCategory, existing.getCategory());
        verify(transactionRepository).save(existing);
    }

    @Test
    void update$shouldUpdateOnlyDescriptionWhenImported() {
        // given — every locked field matches the persisted value, only description differs
        Transaction existing = Transaction.builder()
                .id(5L).user(user).imported(true)
                .amount(BigDecimal.TEN).type(TransactionType.EXPENSE)
                .date(LocalDate.of(2026, 1, 1)).description("old")
                .category(null).build();

        TransactionRequestDTO dto = new TransactionRequestDTO();
        dto.setAmount(BigDecimal.TEN);
        dto.setType(TransactionType.EXPENSE);
        dto.setDate(LocalDate.of(2026, 1, 1));
        dto.setDescription("new description");
        dto.setCategoryId(null);

        when(authService.getAuthenticatedUser()).thenReturn(user);
        when(transactionRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(transactionRepository.save(existing)).thenReturn(existing);
        when(transactionMapper.toDTO(existing)).thenReturn(responseDTO);

        // when
        TransactionResponseDTO result = transactionService.update(5L, dto);

        // then
        assertEquals(responseDTO, result);
        assertEquals("new description", existing.getDescription());
        assertEquals(BigDecimal.TEN, existing.getAmount());
        verify(categoryService, never()).getIfAvailableForUser(any(), any());
        verify(transactionRepository).save(existing);
    }

    @Test
    void update$shouldThrowConflictWhenImportedAndLockedFieldChanged() {
        // given
        Transaction existing = Transaction.builder()
                .id(5L).user(user).imported(true)
                .amount(BigDecimal.TEN).type(TransactionType.EXPENSE)
                .date(LocalDate.of(2026, 1, 1)).description("old")
                .build();

        TransactionRequestDTO dto = new TransactionRequestDTO();
        dto.setAmount(BigDecimal.valueOf(50)); // changed
        dto.setType(TransactionType.EXPENSE);
        dto.setDate(LocalDate.of(2026, 1, 1));
        dto.setDescription("new description");

        when(authService.getAuthenticatedUser()).thenReturn(user);
        when(transactionRepository.findById(5L)).thenReturn(Optional.of(existing));

        // when + then
        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> transactionService.update(5L, dto)
        );

        assertEquals("Transações importadas só permitem editar a descrição", exception.getMessage());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void update$shouldThrowNotFoundWhenTransactionDoesNotExist() {
        // given
        when(authService.getAuthenticatedUser()).thenReturn(user);
        when(transactionRepository.findById(99L)).thenReturn(Optional.empty());

        // when + then
        assertThrows(
                NotFoundException.class,
                () -> transactionService.update(99L, new TransactionRequestDTO())
        );
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void update$shouldThrowForbiddenWhenNotOwner() {
        // given — transaction owned by user 2, authenticated user is 1
        Transaction existing = Transaction.builder()
                .id(5L).user(user(2L)).imported(false)
                .amount(BigDecimal.TEN).type(TransactionType.EXPENSE)
                .date(LocalDate.now()).build();

        when(authService.getAuthenticatedUser()).thenReturn(user);
        when(transactionRepository.findById(5L)).thenReturn(Optional.of(existing));

        // when + then
        assertThrows(
                ForbiddenException.class,
                () -> transactionService.update(5L, new TransactionRequestDTO())
        );
        verify(transactionRepository, never()).save(any());
    }

    // ===== DELETE =====
    @Test
    void delete$shouldThrowConflictWhenTransactionBelongsToBill() {
        // given
        CreditCardBill bill = CreditCardBill.builder().id(7L).build();
        Transaction existing = Transaction.builder()
                .id(5L).user(user).bill(bill).build();

        when(authService.getAuthenticatedUser()).thenReturn(user);
        when(transactionRepository.findById(5L)).thenReturn(Optional.of(existing));

        // when + then
        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> transactionService.delete(5L)
        );

        assertEquals("Transações de fatura não podem ser removidas", exception.getMessage());
        verify(transactionRepository, never()).delete(any());
    }

    // ===== UTILITY METHODS =====
    private User user(Long id) {
        return User.builder()
                .id(id)
                .build();
    }

    private Category category(Long id) {
        return Category.builder()
                .id(id)
                .build();
    }

    private void mockDefaultFlow() {
        when(authService.getAuthenticatedUser()).thenReturn(user);

        when(categoryService.getIfAvailableForUser(category.getId(), user.getId()))
                .thenReturn(category);

        when(transactionMapper.toEntity(requestDTO, user, category))
                .thenReturn(transaction);
    }
}