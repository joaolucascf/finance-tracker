package com.joaolucas.finance_tracker.mapper;


import org.springframework.stereotype.Component;

import com.joaolucas.finance_tracker.dto.category.CategoryRequestDTO;
import com.joaolucas.finance_tracker.dto.category.CategoryResponseDTO;
import com.joaolucas.finance_tracker.entity.Category;
import com.joaolucas.finance_tracker.entity.User;

import jakarta.annotation.Nullable;


@Component
public class CategoryMapper {

    public CategoryResponseDTO toDTO(@Nullable Category category) {
        return category != null
               ? CategoryResponseDTO.builder()
                       .id(category.getId())
                       .name(category.getName())
                       .build()
               : null;
    }

    public Category toEntity(CategoryRequestDTO dto, User user) {
        return Category.builder()
                .name(dto.getName())
                .user(user)
                .build();
    }
}
