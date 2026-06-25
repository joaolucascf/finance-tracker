package com.joaolucas.finance_tracker.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.joaolucas.finance_tracker.entity.AccountType;
import com.joaolucas.finance_tracker.entity.BillStatus;
import com.joaolucas.finance_tracker.entity.Category;
import com.joaolucas.finance_tracker.entity.ConnectionStatus;
import com.joaolucas.finance_tracker.entity.CreditCardBill;
import com.joaolucas.finance_tracker.entity.FinancialAccount;
import com.joaolucas.finance_tracker.entity.FinancialConnection;
import com.joaolucas.finance_tracker.entity.Transaction;
import com.joaolucas.finance_tracker.entity.TransactionType;
import com.joaolucas.finance_tracker.entity.User;
import com.joaolucas.finance_tracker.exception.ConflictException;
import com.joaolucas.finance_tracker.exception.ForbiddenException;
import com.joaolucas.finance_tracker.exception.NotFoundException;
import com.joaolucas.finance_tracker.repository.CreditCardBillRepository;
import com.joaolucas.finance_tracker.repository.FinancialAccountRepository;
import com.joaolucas.finance_tracker.repository.FinancialConnectionRepository;
import com.joaolucas.finance_tracker.repository.TransactionRepository;
import com.joaolucas.finance_tracker.service.AuthService;
import com.joaolucas.finance_tracker.service.CategoryService;

/**
 * Dev/test-only CRUD over the "imported" concepts so credit-card bills and purchases can be mocked by hand
 * without depending on the Open Finance provider. Everything is scoped to the authenticated user.
 */
@RestController
@RequestMapping("/mock")
public class MockController {

    private final AuthService authService;
    private final FinancialConnectionRepository connectionRepository;
    private final FinancialAccountRepository accountRepository;
    private final CreditCardBillRepository billRepository;
    private final TransactionRepository transactionRepository;
    private final CategoryService categoryService;

    public MockController(AuthService authService,
            FinancialConnectionRepository connectionRepository,
            FinancialAccountRepository accountRepository,
            CreditCardBillRepository billRepository,
            TransactionRepository transactionRepository,
            CategoryService categoryService) {
        this.authService = authService;
        this.connectionRepository = connectionRepository;
        this.accountRepository = accountRepository;
        this.billRepository = billRepository;
        this.transactionRepository = transactionRepository;
        this.categoryService = categoryService;
    }

    // ===================== CONNECTIONS =====================

    @GetMapping("/connections")
    public List<ConnectionDTO> listConnections() {
        Long userId = authService.getAuthenticatedUser().getId();
        return connectionRepository.findByUserId(userId).stream().map(this::toDTO).toList();
    }

    @PostMapping("/connections")
    @ResponseStatus(HttpStatus.CREATED)
    public ConnectionDTO createConnection(@RequestBody ConnectionRequest req) {
        User user = authService.getAuthenticatedUser();
        FinancialConnection c = FinancialConnection.builder()
                .user(user)
                .institutionName(orDefault(req.institutionName(), "Banco Mock"))
                .provider(orDefault(req.provider(), "PLUGGY"))
                .externalItemId("mock-" + UUID.randomUUID())
                .status(ConnectionStatus.valueOf(orDefault(req.status(), "ACTIVE")))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return toDTO(connectionRepository.save(c));
    }

    @PutMapping("/connections/{id}")
    public ConnectionDTO updateConnection(@PathVariable Long id, @RequestBody ConnectionRequest req) {
        FinancialConnection c = ownedConnection(id);
        if (req.institutionName() != null) c.setInstitutionName(req.institutionName());
        if (req.provider() != null) c.setProvider(req.provider());
        if (req.status() != null) c.setStatus(ConnectionStatus.valueOf(req.status()));
        c.setUpdatedAt(LocalDateTime.now());
        return toDTO(connectionRepository.save(c));
    }

    @DeleteMapping("/connections/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteConnection(@PathVariable Long id) {
        connectionRepository.delete(ownedConnection(id));
    }

    // ===================== ACCOUNTS =====================

    @GetMapping("/accounts")
    public List<AccountDTO> listAccounts() {
        Long userId = authService.getAuthenticatedUser().getId();
        return accountRepository.findByUserId(userId).stream().map(this::toDTO).toList();
    }

    @PostMapping("/accounts")
    @ResponseStatus(HttpStatus.CREATED)
    public AccountDTO createAccount(@RequestBody AccountRequest req) {
        User user = authService.getAuthenticatedUser();
        FinancialConnection connection = ownedConnection(req.connectionId());
        FinancialAccount a = FinancialAccount.builder()
                .user(user)
                .connection(connection)
                .externalAccountId("mock-" + UUID.randomUUID())
                .name(orDefault(req.name(), "Conta Mock"))
                .type(AccountType.valueOf(orDefault(req.type(), "CARTAO_CREDITO")))
                .currentBalance(req.currentBalance() != null ? req.currentBalance() : BigDecimal.ZERO)
                .currency(orDefault(req.currency(), "BRL"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return toDTO(accountRepository.save(a));
    }

    @PutMapping("/accounts/{id}")
    public AccountDTO updateAccount(@PathVariable Long id, @RequestBody AccountRequest req) {
        FinancialAccount a = ownedAccount(id);
        if (req.connectionId() != null) a.setConnection(ownedConnection(req.connectionId()));
        if (req.name() != null) a.setName(req.name());
        if (req.type() != null) a.setType(AccountType.valueOf(req.type()));
        if (req.currentBalance() != null) a.setCurrentBalance(req.currentBalance());
        if (req.currency() != null) a.setCurrency(req.currency());
        a.setUpdatedAt(LocalDateTime.now());
        return toDTO(accountRepository.save(a));
    }

    @DeleteMapping("/accounts/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAccount(@PathVariable Long id) {
        accountRepository.delete(ownedAccount(id));
    }

    // ===================== BILLS =====================

    @GetMapping("/bills")
    public List<BillDTO> listBills() {
        Long userId = authService.getAuthenticatedUser().getId();
        return billRepository.findByAccount_UserIdOrderByDueDateAsc(userId).stream().map(this::toDTO).toList();
    }

    @PostMapping("/bills")
    @ResponseStatus(HttpStatus.CREATED)
    public BillDTO createBill(@RequestBody BillRequest req) {
        FinancialAccount account = creditCardAccount(req.accountId());
        CreditCardBill b = CreditCardBill.builder()
                .account(account)
                .provider(orDefault(account.getConnection().getProvider(), "PLUGGY"))
                .externalBillId("mock-" + UUID.randomUUID())
                .dueDate(req.dueDate())
                .totalAmount(req.totalAmount() != null ? req.totalAmount() : BigDecimal.ZERO)
                .status(BillStatus.valueOf(orDefault(req.status(), "OPEN")))
                .sequence(req.sequence() != null ? req.sequence() : 1)
                .customName(req.customName())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return toDTO(billRepository.save(b));
    }

    @PutMapping("/bills/{id}")
    public BillDTO updateBill(@PathVariable Long id, @RequestBody BillRequest req) {
        CreditCardBill b = ownedBill(id);
        if (req.accountId() != null) b.setAccount(creditCardAccount(req.accountId()));
        if (req.dueDate() != null) b.setDueDate(req.dueDate());
        if (req.totalAmount() != null) b.setTotalAmount(req.totalAmount());
        if (req.status() != null) b.setStatus(BillStatus.valueOf(req.status()));
        if (req.sequence() != null) b.setSequence(req.sequence());
        b.setCustomName(req.customName());
        b.setUpdatedAt(LocalDateTime.now());
        return toDTO(billRepository.save(b));
    }

    @DeleteMapping("/bills/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBill(@PathVariable Long id) {
        billRepository.delete(ownedBill(id));
    }

    // ===================== TRANSACTIONS =====================

    @GetMapping("/transactions")
    public List<TransactionDTO> listTransactions() {
        Long userId = authService.getAuthenticatedUser().getId();
        return transactionRepository.findByUserIdOrderByIdDesc(userId).stream().map(this::toDTO).toList();
    }

    @PostMapping("/transactions")
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionDTO createTransaction(@RequestBody TransactionRequest req) {
        User user = authService.getAuthenticatedUser();
        Transaction t = Transaction.builder()
                .user(user)
                .description(req.description())
                .amount(req.amount())
                .type(TransactionType.valueOf(orDefault(req.type(), "EXPENSE")))
                .date(req.date())
                .imported(req.imported() != null ? req.imported() : true)
                .category(resolveCategory(req.categoryId(), user.getId()))
                .sourceAccount(req.sourceAccountId() != null ? ownedAccount(req.sourceAccountId()) : null)
                .bill(req.billId() != null ? ownedBill(req.billId()) : null)
                .installmentNumber(req.installmentNumber())
                .totalInstallments(req.totalInstallments())
                .build();
        return toDTO(transactionRepository.save(t));
    }

    @PutMapping("/transactions/{id}")
    public TransactionDTO updateTransaction(@PathVariable Long id, @RequestBody TransactionRequest req) {
        Transaction t = ownedTransaction(id);
        if (req.description() != null) t.setDescription(req.description());
        if (req.amount() != null) t.setAmount(req.amount());
        if (req.type() != null) t.setType(TransactionType.valueOf(req.type()));
        if (req.date() != null) t.setDate(req.date());
        if (req.imported() != null) t.setImported(req.imported());
        if (req.categoryId() != null) t.setCategory(resolveCategory(req.categoryId(), t.getUser().getId()));
        t.setSourceAccount(req.sourceAccountId() != null ? ownedAccount(req.sourceAccountId()) : t.getSourceAccount());
        t.setBill(req.billId() != null ? ownedBill(req.billId()) : null);
        t.setInstallmentNumber(req.installmentNumber());
        t.setTotalInstallments(req.totalInstallments());
        return toDTO(transactionRepository.save(t));
    }

    @DeleteMapping("/transactions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTransaction(@PathVariable Long id) {
        transactionRepository.delete(ownedTransaction(id));
    }

    // ===================== helpers =====================

    private Category resolveCategory(Long categoryId, Long userId) {
        return categoryId != null ? categoryService.getIfAvailableForUser(categoryId, userId) : null;
    }

    private FinancialConnection ownedConnection(Long id) {
        FinancialConnection c = connectionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Conexão não encontrada"));
        assertOwner(c.getUser().getId());
        return c;
    }

    private FinancialAccount ownedAccount(Long id) {
        FinancialAccount a = accountRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Conta não encontrada"));
        assertOwner(a.getUser().getId());
        return a;
    }

    private FinancialAccount creditCardAccount(Long id) {
        if (id == null) {
            throw new ConflictException("Selecione a conta de cartão de crédito da fatura");
        }
        FinancialAccount a = ownedAccount(id);
        if (a.getType() != AccountType.CARTAO_CREDITO) {
            throw new ConflictException("Faturas só podem ser criadas para contas de cartão de crédito");
        }
        return a;
    }

    private CreditCardBill ownedBill(Long id) {
        CreditCardBill b = billRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Fatura não encontrada"));
        assertOwner(b.getAccount().getUser().getId());
        return b;
    }

    private Transaction ownedTransaction(Long id) {
        Transaction t = transactionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Transação não encontrada"));
        assertOwner(t.getUser().getId());
        return t;
    }

    private void assertOwner(Long ownerId) {
        if (!ownerId.equals(authService.getAuthenticatedUser().getId())) {
            throw new ForbiddenException("Acesso negado");
        }
    }

    private static String orDefault(String value, String fallback) {
        return value != null && !value.isBlank() ? value : fallback;
    }

    // ===================== DTOs =====================

    private ConnectionDTO toDTO(FinancialConnection c) {
        return new ConnectionDTO(c.getId(), c.getInstitutionName(), c.getProvider(),
                c.getExternalItemId(), c.getStatus().name());
    }

    private AccountDTO toDTO(FinancialAccount a) {
        return new AccountDTO(a.getId(), a.getConnection().getId(), a.getName(), a.getType().name(),
                a.getCurrentBalance(), a.getCurrency(), a.getExternalAccountId());
    }

    private BillDTO toDTO(CreditCardBill b) {
        return new BillDTO(b.getId(), b.getAccount().getId(), b.getDueDate(), b.getTotalAmount(),
                b.getStatus().name(), b.getSequence(), b.getCustomName(), b.getExternalBillId());
    }

    private TransactionDTO toDTO(Transaction t) {
        return new TransactionDTO(t.getId(), t.getDescription(), t.getAmount(), t.getType().name(), t.getDate(),
                t.isImported(),
                t.getSourceAccount() != null ? t.getSourceAccount().getId() : null,
                t.getBill() != null ? t.getBill().getId() : null,
                t.getCategory() != null ? t.getCategory().getId() : null,
                t.getInstallmentNumber(), t.getTotalInstallments());
    }

    public record ConnectionRequest(String institutionName, String provider, String status) {}
    public record ConnectionDTO(Long id, String institutionName, String provider, String externalItemId, String status) {}

    public record AccountRequest(Long connectionId, String name, String type, BigDecimal currentBalance, String currency) {}
    public record AccountDTO(Long id, Long connectionId, String name, String type, BigDecimal currentBalance,
                             String currency, String externalAccountId) {}

    public record BillRequest(Long accountId, LocalDate dueDate, BigDecimal totalAmount, String status,
                              Integer sequence, String customName) {}
    public record BillDTO(Long id, Long accountId, LocalDate dueDate, BigDecimal totalAmount, String status,
                          Integer sequence, String customName, String externalBillId) {}

    public record TransactionRequest(String description, BigDecimal amount, String type, LocalDate date,
                                     Boolean imported, Long sourceAccountId, Long billId, Long categoryId,
                                     Integer installmentNumber, Integer totalInstallments) {}
    public record TransactionDTO(Long id, String description, BigDecimal amount, String type, LocalDate date,
                                 boolean imported, Long sourceAccountId, Long billId, Long categoryId,
                                 Integer installmentNumber, Integer totalInstallments) {}
}
