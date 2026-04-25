package com.joaolucas.finance_tracker.service;


import java.util.List;

import org.springframework.stereotype.Service;

import com.joaolucas.finance_tracker.dto.transaction.TransactionRequestDTO;
import com.joaolucas.finance_tracker.dto.transaction.TransactionResponseDTO;
import com.joaolucas.finance_tracker.entity.Category;
import com.joaolucas.finance_tracker.entity.Transaction;
import com.joaolucas.finance_tracker.entity.User;
import com.joaolucas.finance_tracker.mapper.TransactionMapper;
import com.joaolucas.finance_tracker.repository.TransactionRepository;


@Service
public class TransactionService {

    private final AuthService authService;
    private final CategoryService categoryService;
    private final TransactionMapper transactionMapper;
    private final TransactionRepository transactionRepository;

    public TransactionService(AuthService authService, CategoryService categoryService,
            TransactionMapper transactionMapper,
            TransactionRepository transactionRepository) {
        this.authService = authService;
        this.categoryService = categoryService;
        this.transactionMapper = transactionMapper;
        this.transactionRepository = transactionRepository;
    }

    public TransactionResponseDTO create(TransactionRequestDTO requestDTO) {

        User transactionOwner = this.authService.getAuthenticatedUser();
        Category transactionCategory = this.categoryService.getIfAvailableForUser(requestDTO.getCategoryId(),
                transactionOwner.getId());

        Transaction transaction = transactionMapper.toEntity(requestDTO, transactionOwner, transactionCategory);

        Transaction saved = this.transactionRepository.save(transaction);

        return transactionMapper.toDTO(saved);
    }

    public List<TransactionResponseDTO> getByAuthenticatedUser() {
        User authenticadedUser = this.authService.getAuthenticatedUser();
        return transactionRepository.findByUserId(authenticadedUser.getId())
                .stream()
                .map(transactionMapper::toDTO)
                .toList();
    }
}