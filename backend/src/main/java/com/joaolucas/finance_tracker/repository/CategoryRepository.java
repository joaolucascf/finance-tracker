package com.joaolucas.finance_tracker.repository;


import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.joaolucas.finance_tracker.entity.Category;


public interface CategoryRepository extends JpaRepository<Category, Long> {

    @Query ("""
                SELECT c FROM Category c
                WHERE c.user.id = :userId
                OR c.defaultCategory = true
            """)
    List<Category> findAllForUser(Long userId);
}
