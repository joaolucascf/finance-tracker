package com.joaolucas.finance_tracker.service;

import com.joaolucas.finance_tracker.entity.*;
import com.joaolucas.finance_tracker.openfinance.AccountData;
import com.joaolucas.finance_tracker.openfinance.BillData;
import com.joaolucas.finance_tracker.openfinance.OpenFinanceProvider;
import com.joaolucas.finance_tracker.openfinance.TransactionData;
import com.joaolucas.finance_tracker.repository.CreditCardBillRepository;
import com.joaolucas.finance_tracker.repository.FinancialAccountRepository;
import com.joaolucas.finance_tracker.repository.FinancialConnectionRepository;
import com.joaolucas.finance_tracker.repository.ImportedTransactionRepository;
import com.joaolucas.finance_tracker.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
public class OpenFinanceSyncService {

    private static final Logger log = LoggerFactory.getLogger(OpenFinanceSyncService.class);
    private static final int INITIAL_SYNC_DAYS = 90;

    private final OpenFinanceProvider provider;
    private final FinancialConnectionRepository connectionRepository;
    private final FinancialAccountRepository accountRepository;
    private final ImportedTransactionRepository importedTransactionRepository;
    private final TransactionRepository transactionRepository;
    private final CreditCardBillRepository billRepository;

    public OpenFinanceSyncService(OpenFinanceProvider provider,
                                  FinancialConnectionRepository connectionRepository,
                                  FinancialAccountRepository accountRepository,
                                  ImportedTransactionRepository importedTransactionRepository,
                                  TransactionRepository transactionRepository,
                                  CreditCardBillRepository billRepository) {
        this.provider = provider;
        this.connectionRepository = connectionRepository;
        this.accountRepository = accountRepository;
        this.importedTransactionRepository = importedTransactionRepository;
        this.transactionRepository = transactionRepository;
        this.billRepository = billRepository;
    }

    @Transactional
    public void syncConnection(FinancialConnection connection) {
        syncConnection(connection, LocalDate.now().minusDays(INITIAL_SYNC_DAYS));
    }

    @Transactional
    public void syncConnection(FinancialConnection connection, LocalDate from) {
        try {
            List<AccountData> accounts = provider.getAccounts(connection.getExternalItemId());
            LocalDate to = LocalDate.now();

            for (AccountData accountData : accounts) {
                FinancialAccount account = accountRepository
                        .findByExternalAccountId(accountData.externalId())
                        .orElseGet(() -> {
                            FinancialAccount newAccount = FinancialAccount.builder()
                                    .user(connection.getUser())
                                    .connection(connection)
                                    .externalAccountId(accountData.externalId())
                                    .name(accountData.name())
                                    .type(accountData.type())
                                    .currency(accountData.currency())
                                    .currentBalance(accountData.balance())
                                    .createdAt(LocalDateTime.now())
                                    .updatedAt(LocalDateTime.now())
                                    .build();
                            return accountRepository.save(newAccount);
                        });

                account.setCurrentBalance(accountData.balance());
                account.setUpdatedAt(LocalDateTime.now());
                accountRepository.save(account);

                if (account.getType() == AccountType.CARTAO_CREDITO) {
                    syncBills(account, connection.getProvider());
                    importCardTransactions(account, connection.getProvider(), from, to);
                    syncTransitionBill(account, connection.getProvider());
                } else {
                    importNewTransactions(account, connection.getProvider(), from, to);
                }
            }

            recomputeBillSequence(connection.getUser().getId());

            connection.setStatus(ConnectionStatus.ACTIVE);
            connection.setUpdatedAt(LocalDateTime.now());
            connectionRepository.save(connection);

        } catch (Exception e) {
            log.error("Sync failed for connection {} ({}): {}", connection.getId(),
                    connection.getInstitutionName(), e.getMessage());
            connection.setStatus(ConnectionStatus.ERROR);
            connection.setUpdatedAt(LocalDateTime.now());
            connectionRepository.save(connection);
        }
    }

    /**
     * Upserts the account's provider bills. All provider bills are CLOSED by definition — the provider only
     * returns a bill once the statement has closed (the still-open cycle has no provider bill yet; it is
     * represented by the transition bill, see {@link #syncTransitionBill}). Bills that fall out of the
     * provider's 12-bill window are never deleted, so we become the frozen source of truth for them.
     */
    private void syncBills(FinancialAccount account, String providerName) {
        for (BillData billData : provider.getBills(account.getExternalAccountId())) {
            CreditCardBill bill = billRepository
                    .findByProviderAndExternalBillId(providerName, billData.externalId())
                    .orElseGet(() -> CreditCardBill.builder()
                            .account(account)
                            .provider(providerName)
                            .externalBillId(billData.externalId())
                            .createdAt(LocalDateTime.now())
                            .build());

            bill.setDueDate(billData.dueDate());
            bill.setTotalAmount(billData.totalAmount());
            bill.setStatus(BillStatus.CLOSED);
            bill.setUpdatedAt(LocalDateTime.now());
            billRepository.save(bill);
        }
    }

    /**
     * Maintains the single open transition bill for a card — the current cycle the provider hasn't billed
     * yet. Its total is the sum of the card's EXPENSE purchases with no bill (orphans), and its due date is
     * the most recent closed bill's due date plus one month (NULL when the card has no closed bill yet).
     */
    private void syncTransitionBill(FinancialAccount account, String providerName) {
        CreditCardBill transition = billRepository
                .findByAccountIdAndExternalBillIdIsNull(account.getId())
                .orElseGet(() -> CreditCardBill.builder()
                        .account(account)
                        .provider(providerName)
                        .status(BillStatus.OPEN)
                        .createdAt(LocalDateTime.now())
                        .build());

        BigDecimal total = transactionRepository
                .findBySourceAccount_IdAndBillIsNullAndTypeOrderByDateDescIdAsc(account.getId(), TransactionType.EXPENSE)
                .stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        LocalDate nextDueDate = billRepository.findByAccountId(account.getId()).stream()
                .filter(b -> b.getExternalBillId() != null && b.getDueDate() != null)
                .map(CreditCardBill::getDueDate)
                .max(Comparator.naturalOrder())
                .map(d -> d.plusMonths(1))
                .orElse(null);

        transition.setTotalAmount(total);
        transition.setDueDate(nextDueDate);
        transition.setStatus(BillStatus.OPEN);
        transition.setUpdatedAt(LocalDateTime.now());
        billRepository.save(transition);
    }

    private void importCardTransactions(FinancialAccount account, String providerName,
                                        LocalDate from, LocalDate to) {
        for (TransactionData txData : provider.getTransactions(account.getExternalAccountId(), from, to)) {
            CreditCardBill targetBill = txData.billId() != null
                    ? billRepository.findByProviderAndExternalBillId(providerName, txData.billId()).orElse(null)
                    : null;

            ImportedTransaction existing = importedTransactionRepository
                    .findByProviderAndExternalTransactionId(providerName, txData.externalId())
                    .orElse(null);

            if (existing != null) {
                // Re-derive provider metadata (bill link, category) on every sync — not a user edit.
                Transaction tx = existing.getTransaction();
                if (tx != null) {
                    tx.setBill(targetBill);
                    tx.setProviderCategory(txData.category());
                    transactionRepository.save(tx);
                }
                continue;
            }

            Transaction tx = Transaction.builder()
                    .amount(txData.amount())
                    .type(txData.type())
                    .description(txData.description())
                    .date(txData.date())
                    .user(account.getUser())
                    .imported(true)
                    .sourceAccount(account)
                    .bill(targetBill)
                    .providerCategory(txData.category())
                    .installmentNumber(txData.installmentNumber())
                    .totalInstallments(txData.totalInstallments())
                    .build();
            tx = transactionRepository.save(tx);

            ImportedTransaction imported = ImportedTransaction.builder()
                    .transaction(tx)
                    .externalTransactionId(txData.externalId())
                    .account(account)
                    .provider(providerName)
                    .importedAt(LocalDateTime.now())
                    .build();
            importedTransactionRepository.save(imported);
        }
    }

    /** Numbers the user's closed bills sequentially (#1, #2, ...) by due date; transition bills are skipped. */
    private void recomputeBillSequence(Long userId) {
        List<CreditCardBill> bills = billRepository.findByAccount_UserIdAndExternalBillIdIsNotNullOrderByDueDateAsc(userId);
        int seq = 1;
        for (CreditCardBill bill : bills) {
            if (bill.getSequence() == null || bill.getSequence() != seq) {
                bill.setSequence(seq);
                billRepository.save(bill);
            }
            seq++;
        }
    }

    private void importNewTransactions(FinancialAccount account, String provider, LocalDate from, LocalDate to) {
        List<TransactionData> transactions = this.provider.getTransactions(account.getExternalAccountId(), from, to);

        for (TransactionData txData : transactions) {
            ImportedTransaction existing = importedTransactionRepository
                    .findByProviderAndExternalTransactionId(provider, txData.externalId())
                    .orElse(null);
            if (existing != null) {
                // Backfill/refresh provider category on already-imported rows — metadata, not a user edit.
                Transaction tx = existing.getTransaction();
                if (tx != null && !Objects.equals(tx.getProviderCategory(), txData.category())) {
                    tx.setProviderCategory(txData.category());
                    transactionRepository.save(tx);
                }
                continue;
            }

            Transaction tx = Transaction.builder()
                    .amount(txData.amount())
                    .type(txData.type())
                    .description(txData.description())
                    .date(txData.date())
                    .user(account.getUser())
                    .imported(true)
                    .sourceAccount(account)
                    .providerCategory(txData.category())
                    .build();
            tx = transactionRepository.save(tx);

            ImportedTransaction imported = ImportedTransaction.builder()
                    .transaction(tx)
                    .externalTransactionId(txData.externalId())
                    .account(account)
                    .provider(provider)
                    .importedAt(LocalDateTime.now())
                    .build();
            importedTransactionRepository.save(imported);
        }
    }
}
