package com.joaolucas.finance_tracker.repository;


import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.joaolucas.finance_tracker.entity.Category;


public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByUserIdOrIsDefaultIsTrue(Long userId);
}
