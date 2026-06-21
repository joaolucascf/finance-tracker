package com.joaolucas.finance_tracker.openfinance.pluggy;

import com.joaolucas.finance_tracker.entity.AccountType;
import com.joaolucas.finance_tracker.entity.TransactionType;
import com.joaolucas.finance_tracker.openfinance.AccountData;
import com.joaolucas.finance_tracker.openfinance.ItemStatus;
import com.joaolucas.finance_tracker.openfinance.OpenFinanceProvider;
import com.joaolucas.finance_tracker.openfinance.TransactionData;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class PluggyProvider implements OpenFinanceProvider {

    private static final String PROVIDER_NAME = "PLUGGY";

    private final RestClient restClient;
    private final PluggyProperties properties;

    private String cachedApiKey;
    private Instant apiKeyExpiry = Instant.EPOCH;

    public PluggyProvider(PluggyProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .build();
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public String generateConnectToken(String clientUserId) {
        ConnectTokenResponse response = restClient.post()
                .uri("/connect_token")
                .header("X-API-KEY", getApiKey())
                .body(new ConnectTokenRequest(clientUserId))
                .retrieve()
                .body(ConnectTokenResponse.class);
        return response.accessToken();
    }

    @Override
    public ItemStatus getItemStatus(String externalItemId) {
        ItemResponse item = restClient.get()
                .uri("/items/{id}", externalItemId)
                .header("X-API-KEY", getApiKey())
                .retrieve()
                .body(ItemResponse.class);
        boolean active = "UPDATED".equals(item.status()) || "UPDATING".equals(item.status());
        String name = item.connector() != null ? item.connector().name() : "Instituição";
        return new ItemStatus(item.id(), name, active);
    }

    @Override
    public List<AccountData> getAccounts(String externalItemId) {
        AccountsResponse response = restClient.get()
                .uri("/accounts?itemId={itemId}", externalItemId)
                .header("X-API-KEY", getApiKey())
                .retrieve()
                .body(AccountsResponse.class);
        if (response == null || response.results() == null) return List.of();
        return response.results().stream()
                .map(a -> new AccountData(
                        a.id(),
                        a.name(),
                        mapAccountType(a.type()),
                        BigDecimal.valueOf(a.balance() != null ? a.balance() : 0.0),
                        a.currencyCode() != null ? a.currencyCode() : "BRL"
                ))
                .toList();
    }

    @Override
    public List<TransactionData> getTransactions(String externalAccountId, LocalDate from, LocalDate to) {
        List<TransactionData> all = new ArrayList<>();
        // First page — include date filters
        V2TransactionsResponse response = restClient.get()
                .uri("/v2/transactions?accountId={id}&dateFrom={from}&dateTo={to}",
                        externalAccountId, from, to)
                .header("X-API-KEY", getApiKey())
                .retrieve()
                .body(V2TransactionsResponse.class);

        while (response != null && response.results() != null) {
            for (PluggyTransaction tx : response.results()) {
                all.add(mapTransaction(tx));
            }
            // "next" is a query string like "?accountId=...&after=BASE64..."
            String next = response.next();
            if (next == null || next.isBlank()) break;
            response = restClient.get()
                    .uri("/v2/transactions" + next)
                    .header("X-API-KEY", getApiKey())
                    .retrieve()
                    .body(V2TransactionsResponse.class);
        }
        return all;
    }

    private synchronized String getApiKey() {
        if (cachedApiKey == null || Instant.now().isAfter(apiKeyExpiry.minus(Duration.ofMinutes(5)))) {
            AuthResponse auth = restClient.post()
                    .uri("/auth")
                    .body(new AuthRequest(properties.getClientId(), properties.getClientSecret()))
                    .retrieve()
                    .body(AuthResponse.class);
            cachedApiKey = auth.apiKey();
            apiKeyExpiry = Instant.now().plus(Duration.ofHours(2));
        }
        return cachedApiKey;
    }

    private AccountType mapAccountType(String type) {
        return switch (type != null ? type : "") {
            case "SAVINGS" -> AccountType.POUPANCA;
            case "CREDIT" -> AccountType.CARTAO_CREDITO;
            default -> AccountType.CONTA_CORRENTE;
        };
    }

    private TransactionData mapTransaction(PluggyTransaction tx) {
        TransactionType type = "CREDIT".equals(tx.type()) ? TransactionType.INCOME : TransactionType.EXPENSE;
        BigDecimal amount = BigDecimal.valueOf(Math.abs(tx.amount() != null ? tx.amount() : 0.0));
        LocalDate date = parseDate(tx.date());
        String description = tx.description();
        if (description != null && description.length() > 60) {
            description = description.substring(0, 60);
        }
        return new TransactionData(tx.id(), description, amount, type, date);
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.length() < 10) return LocalDate.now();
        return LocalDate.parse(dateStr.substring(0, 10));
    }

    // --- Internal Pluggy API models ---

    record AuthRequest(String clientId, String clientSecret) {}
    record AuthResponse(String apiKey) {}
    record ConnectTokenRequest(String clientUserId) {}
    record ConnectTokenResponse(String accessToken) {}
    record ItemResponse(String id, String status, Connector connector) {
        record Connector(String name) {}
    }
    record AccountsResponse(List<PluggyAccount> results) {}
    record PluggyAccount(String id, String itemId, String type, String name, Double balance, String currencyCode) {}
    record V2TransactionsResponse(List<PluggyTransaction> results, String next) {}
    record PluggyTransaction(String id, String accountId, String description, Double amount, String date, String type) {}
}
