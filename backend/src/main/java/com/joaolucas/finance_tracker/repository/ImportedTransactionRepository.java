package com.joaolucas.finance_tracker.repository;

import com.joaolucas.finance_tracker.entity.ImportedTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportedTransactionRepository extends JpaRepository<ImportedTransaction, Long> {

    boolean existsByProviderAndExternalTransactionId(String provider, String externalTransactionId);
}
