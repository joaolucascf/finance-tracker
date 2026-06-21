package com.joaolucas.finance_tracker.repository;

import com.joaolucas.finance_tracker.entity.ConnectionStatus;
import com.joaolucas.finance_tracker.entity.FinancialConnection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FinancialConnectionRepository extends JpaRepository<FinancialConnection, Long> {

    List<FinancialConnection> findByUserId(Long userId);

    List<FinancialConnection> findByStatus(ConnectionStatus status);
}
