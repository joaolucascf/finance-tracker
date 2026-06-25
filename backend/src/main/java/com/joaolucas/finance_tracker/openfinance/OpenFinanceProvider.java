package com.joaolucas.finance_tracker.openfinance;

import java.time.LocalDate;
import java.util.List;

public interface OpenFinanceProvider {

    String getProviderName();

    String generateConnectToken(String clientUserId);

    ItemStatus getItemStatus(String externalItemId);

    List<AccountData> getAccounts(String externalItemId);

    List<TransactionData> getTransactions(String externalAccountId, LocalDate from, LocalDate to);

    List<BillData> getBills(String externalAccountId);
}
