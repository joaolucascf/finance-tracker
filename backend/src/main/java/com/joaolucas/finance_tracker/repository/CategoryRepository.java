package com.joaolucas.finance_tracker.repository;


import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.joaolucas.finance_tracker.entity.Category;


public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * Retrieves a category by its ID only if it is accessible to the given user.
     * A category is considered accessible if it either belongs to the user
     * or is a default (global) category.
     *
     * @param categoryId the ID of the category to be retrieved
     * @param userId     the ID of the user requesting access to the category
     * @return an Optional containing the category if it exists and is accessible,
     * or empty otherwise
     */
    @Query ("""
            SELECT c from Category c
            WHERE c.id = :categoryId
            AND (c.user.id = :userId OR c.isDefault = true)
            """)
    Optional<Category> findCategoryForUserIfAvailable(Long categoryId, Long userId);

    List<Category> findByUserIdOrIsDefaultIsTrue(Long userId);
}
