package com.joaolucas.finance_tracker.controller;


import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.joaolucas.finance_tracker.dto.category.CategoryRequestDTO;
import com.joaolucas.finance_tracker.dto.category.CategoryResponseDTO;
import com.joaolucas.finance_tracker.service.CategoryService;

import jakarta.validation.Valid;


@RestController
@RequestMapping ("/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping ("/default")
    public CategoryResponseDTO createDefault(@RequestBody @Valid CategoryRequestDTO category) {
        return this.categoryService.createDefault(category);
    }

    @PostMapping
    public CategoryResponseDTO createForUser(@RequestBody @Valid CategoryRequestDTO category) {
        return this.categoryService.createForUser(category);
    }

    @GetMapping
    public List<CategoryResponseDTO> getAvailableForAuthenticatedUser() {
        return this.categoryService.getAvailableCategoriesForAuthenticatedUsers();
    }
}
