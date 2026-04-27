package com.joaolucas.finance_tracker.service;


import java.util.List;

import org.springframework.stereotype.Service;

import com.joaolucas.finance_tracker.dto.category.CategoryRequestDTO;
import com.joaolucas.finance_tracker.dto.category.CategoryResponseDTO;
import com.joaolucas.finance_tracker.entity.Category;
import com.joaolucas.finance_tracker.entity.User;
import com.joaolucas.finance_tracker.exception.ForbiddenException;
import com.joaolucas.finance_tracker.exception.NotFoundException;
import com.joaolucas.finance_tracker.mapper.CategoryMapper;
import com.joaolucas.finance_tracker.repository.CategoryRepository;


@Service
public class CategoryService {

    private final AuthService authService;
    private final CategoryMapper categoryMapper;
    private final CategoryRepository categoryRepository;

    public CategoryService(AuthService authService, CategoryMapper categoryMapper,
            CategoryRepository categoryRepository) {
        this.authService = authService;
        this.categoryMapper = categoryMapper;
        this.categoryRepository = categoryRepository;
    }

    public CategoryResponseDTO createDefault(CategoryRequestDTO requestDTO) {

        Category category = this.categoryMapper.toEntity(requestDTO, null);

        Category saved = categoryRepository.save(category);

        return this.categoryMapper.toDTO(saved);
    }

    public CategoryResponseDTO createForUser(CategoryRequestDTO requestDTO) {

        User categoryOwner = this.authService.getAuthenticatedUser();

        Category category = this.categoryMapper.toEntity(requestDTO, categoryOwner);

        Category saved = categoryRepository.save(category);

        return this.categoryMapper.toDTO(saved);
    }

    public List<CategoryResponseDTO> getAvailableCategoriesForAuthenticatedUsers() {
        User user = this.authService.getAuthenticatedUser();
        return this.categoryRepository.findByUserIdOrIsDefaultIsTrue(user.getId())
                .stream().map(this.categoryMapper::toDTO).toList();
    }

    public Category getIfAvailableForUser(Long categoryId, Long userId) {

        if (categoryId == null || userId == null) {
            return null;
        }

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Categoria não encontrada"));

        boolean isOwner = category.getUser().getId().equals(userId);
        boolean isDefault = category.isDefault();

        if (!isOwner && !isDefault) {
            throw new ForbiddenException("Categoria inválida para este usuário");
        }

        return category;
    }
}