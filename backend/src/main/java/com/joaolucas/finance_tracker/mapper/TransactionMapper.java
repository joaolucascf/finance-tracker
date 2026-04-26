package com.joaolucas.finance_tracker.mapper;


import org.springframework.stereotype.Component;

import com.joaolucas.finance_tracker.dto.transaction.TransactionRequestDTO;
import com.joaolucas.finance_tracker.dto.transaction.TransactionResponseDTO;
import com.joaolucas.finance_tracker.entity.Category;
import com.joaolucas.finance_tracker.entity.Transaction;
import com.joaolucas.finance_tracker.entity.TransactionType;
import com.joaolucas.finance_tracker.entity.User;


@Component
public class TransactionMapper {

    CategoryMapper categoryMapper;

    public TransactionMapper(CategoryMapper categoryMapper) {
        this.categoryMapper = categoryMapper;
    }

    public TransactionResponseDTO toDTO(Transaction transaction) {
        return TransactionResponseDTO
                .builder()
                .id(transaction.getId())
                .amount(transaction.getAmount())
                .type(transaction.getType())
                .description(transaction.getDescription())
                .date(transaction.getDate())
                .category(categoryMapper.toDTO(transaction.getCategory()))
                .build();
    }

    public Transaction toEntity(TransactionRequestDTO dto, User user, Category category) {
        return Transaction.builder()
                .amount(dto.getAmount())
                .type(dto.getType())
                .description(dto.getDescription())
                .date(dto.getDate())
                .user(user)
                .category(category)
                .build();
    }
}
