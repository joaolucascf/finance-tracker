package com.joaolucas.finance_tracker.service;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.joaolucas.finance_tracker.dto.transaction.TransactionRequestDTO;
import com.joaolucas.finance_tracker.dto.transaction.TransactionResponseDTO;
import com.joaolucas.finance_tracker.entity.Category;
import com.joaolucas.finance_tracker.entity.Transaction;
import com.joaolucas.finance_tracker.entity.User;
import com.joaolucas.finance_tracker.exception.ForbiddenException;
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

        when(transactionRepository.findByUserId(user.getId()))
                .thenReturn(mockReturn);

        when(transactionMapper.toDTO(transaction))
                .thenReturn(dto);

        // when
        List<TransactionResponseDTO> result = this.transactionService.getByAuthenticatedUser();

        // then
        assertEquals(1, result.size());
        assertEquals(dto, result.get(0));
        verify(transactionMapper).toDTO(any());
    }

    @Test
    void getByAuthenticatedUser$shouldReturnEmptyListWhenUserHasNoTransactions() {
        // given
        when(authService.getAuthenticatedUser()).thenReturn(user);

        when(transactionRepository.findByUserId(user.getId())).thenReturn(Collections.emptyList());

        // when
        List<TransactionResponseDTO> result = this.transactionService.getByAuthenticatedUser();

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
                () -> transactionService.getByAuthenticatedUser()
        );

        assertEquals("Usuário autenticado não encontrado", exception.getMessage());
        verify(transactionRepository, never()).findByUserId(anyLong());
        verify(transactionMapper, never()).toDTO(any());
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