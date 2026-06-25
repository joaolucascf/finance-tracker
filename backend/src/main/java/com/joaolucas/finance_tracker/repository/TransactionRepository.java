package com.joaolucas.finance_tracker.repository;


import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.joaolucas.finance_tracker.entity.AccountType;
import com.joaolucas.finance_tracker.entity.Transaction;
import com.joaolucas.finance_tracker.entity.TransactionType;


public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    /**
     * Main-ledger entries for a period: standalone transactions (not tied to a bill), excluding
     * (a) any whose provider category is hidden (e.g. investments) and (b) credit-card transactions —
     * card movements only ever appear aggregated inside a bill (closed or transition), never loose.
     * Manual transactions (null provider category, null source account) are kept.
     */
    @Query("""
            SELECT t FROM Transaction t
            WHERE t.user.id = :userId AND t.bill IS NULL
              AND t.date BETWEEN :start AND :end
              AND (t.providerCategory IS NULL OR LOWER(t.providerCategory) NOT IN :hiddenCategories)
              AND (t.sourceAccount IS NULL OR t.sourceAccount.type <> :cardType)
            ORDER BY t.date DESC, t.id ASC
            """)
    List<Transaction> findVisibleLedgerEntries(
            @Param("userId") Long userId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end,
            @Param("hiddenCategories") Collection<String> hiddenCategories,
            @Param("cardType") AccountType cardType);

    /** EXPENSE purchases of a card that have no provider bill yet — the items/total of its transition bill. */
    List<Transaction> findBySourceAccount_IdAndBillIsNullAndTypeOrderByDateDescIdAsc(
            Long accountId, TransactionType type);

    List<Transaction> findByBillIdOrderByDateDescIdAsc(Long billId);

    List<Transaction> findByUserIdOrderByIdDesc(Long userId);
}
