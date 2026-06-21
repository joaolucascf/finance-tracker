package com.joaolucas.finance_tracker.service;

import com.joaolucas.finance_tracker.entity.*;
import com.joaolucas.finance_tracker.openfinance.AccountData;
import com.joaolucas.finance_tracker.openfinance.OpenFinanceProvider;
import com.joaolucas.finance_tracker.openfinance.TransactionData;
import com.joaolucas.finance_tracker.repository.FinancialAccountRepository;
import com.joaolucas.finance_tracker.repository.FinancialConnectionRepository;
import com.joaolucas.finance_tracker.repository.ImportedTransactionRepository;
import com.joaolucas.finance_tracker.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OpenFinanceSyncService {

    private static final Logger log = LoggerFactory.getLogger(OpenFinanceSyncService.class);
    private static final int INITIAL_SYNC_DAYS = 90;

    private final OpenFinanceProvider provider;
    private final FinancialConnectionRepository connectionRepository;
    private final FinancialAccountRepository accountRepository;
    private final ImportedTransactionRepository importedTransactionRepository;
    private final TransactionRepository transactionRepository;

    public OpenFinanceSyncService(OpenFinanceProvider provider,
                                  FinancialConnectionRepository connectionRepository,
                                  FinancialAccountRepository accountRepository,
                                  ImportedTransactionRepository importedTransactionRepository,
                                  TransactionRepository transactionRepository) {
        this.provider = provider;
        this.connectionRepository = connectionRepository;
        this.accountRepository = accountRepository;
        this.importedTransactionRepository = importedTransactionRepository;
        this.transactionRepository = transactionRepository;
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

                importNewTransactions(account, connection.getProvider(), from, to);
            }

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

    private void importNewTransactions(FinancialAccount account, String provider, LocalDate from, LocalDate to) {
        List<TransactionData> transactions = this.provider.getTransactions(account.getExternalAccountId(), from, to);

        for (TransactionData txData : transactions) {
            if (importedTransactionRepository.existsByProviderAndExternalTransactionId(provider, txData.externalId())) {
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
