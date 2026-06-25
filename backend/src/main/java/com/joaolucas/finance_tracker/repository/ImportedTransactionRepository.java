package com.joaolucas.finance_tracker.repository;

import java.util.Optional;

import com.joaolucas.finance_tracker.entity.ImportedTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportedTransactionRepository extends JpaRepository<ImportedTransaction, Long> {

    Optional<ImportedTransaction> findByProviderAndExternalTransactionId(String provider, String externalTransactionId);
}
