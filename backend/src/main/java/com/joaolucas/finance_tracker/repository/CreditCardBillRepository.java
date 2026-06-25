package com.joaolucas.finance_tracker.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.joaolucas.finance_tracker.entity.CreditCardBill;

public interface CreditCardBillRepository extends JpaRepository<CreditCardBill, Long> {

    Optional<CreditCardBill> findByProviderAndExternalBillId(String provider, String externalBillId);

    /** The single open transition bill of an account (current cycle), if it exists. */
    Optional<CreditCardBill> findByAccountIdAndExternalBillIdIsNull(Long accountId);

    List<CreditCardBill> findByAccountId(Long accountId);

    List<CreditCardBill> findByAccount_UserIdOrderByDueDateAsc(Long userId);

    /** Closed (provider) bills only — excludes transition bills, which have no sequence. */
    List<CreditCardBill> findByAccount_UserIdAndExternalBillIdIsNotNullOrderByDueDateAsc(Long userId);

    List<CreditCardBill> findByAccount_UserIdAndDueDateBetweenOrderByDueDateDesc(
            Long userId, LocalDate start, LocalDate end);
}
