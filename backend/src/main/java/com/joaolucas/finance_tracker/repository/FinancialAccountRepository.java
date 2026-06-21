package com.joaolucas.finance_tracker.repository;

import com.joaolucas.finance_tracker.entity.FinancialAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FinancialAccountRepository extends JpaRepository<FinancialAccount, Long> {

    List<FinancialAccount> findByUserId(Long userId);

    List<FinancialAccount> findByConnectionId(Long connectionId);

    Optional<FinancialAccount> findByExternalAccountId(String externalAccountId);
}
