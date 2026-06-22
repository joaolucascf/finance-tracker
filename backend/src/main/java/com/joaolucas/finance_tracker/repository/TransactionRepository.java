package com.joaolucas.finance_tracker.repository;


import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.joaolucas.finance_tracker.entity.Transaction;


public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByUserIdAndDateBetweenOrderByDateDescIdAsc(Long userId, LocalDate start, LocalDate end);
}
