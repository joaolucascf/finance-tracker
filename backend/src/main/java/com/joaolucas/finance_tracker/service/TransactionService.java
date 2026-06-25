package com.joaolucas.finance_tracker.service;


import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.joaolucas.finance_tracker.dto.bill.BillResponseDTO;
import com.joaolucas.finance_tracker.dto.transaction.LedgerEntryDTO;
import com.joaolucas.finance_tracker.dto.transaction.TransactionRequestDTO;
import com.joaolucas.finance_tracker.dto.transaction.TransactionResponseDTO;
import com.joaolucas.finance_tracker.entity.AccountType;
import com.joaolucas.finance_tracker.entity.Category;
import com.joaolucas.finance_tracker.entity.Transaction;
import com.joaolucas.finance_tracker.entity.User;
import com.joaolucas.finance_tracker.exception.ConflictException;
import com.joaolucas.finance_tracker.exception.ForbiddenException;
import com.joaolucas.finance_tracker.exception.NotFoundException;
import com.joaolucas.finance_tracker.mapper.TransactionMapper;
import com.joaolucas.finance_tracker.openfinance.PluggyCategory;
import com.joaolucas.finance_tracker.repository.TransactionRepository;


@Service
public class TransactionService {

    private final AuthService authService;
    private final CategoryService categoryService;
    private final TransactionMapper transactionMapper;
    private final TransactionRepository transactionRepository;
    private final BillService billService;

    public TransactionService(AuthService authService, CategoryService categoryService,
            TransactionMapper transactionMapper,
            TransactionRepository transactionRepository,
            BillService billService) {
        this.authService = authService;
        this.categoryService = categoryService;
        this.transactionMapper = transactionMapper;
        this.transactionRepository = transactionRepository;
        this.billService = billService;
    }

    public TransactionResponseDTO create(TransactionRequestDTO requestDTO) {

        User transactionOwner = this.authService.getAuthenticatedUser();
        Category transactionCategory = this.categoryService.getIfAvailableForUser(requestDTO.getCategoryId(),
                transactionOwner.getId());

        Transaction transaction = transactionMapper.toEntity(requestDTO, transactionOwner, transactionCategory);

        Transaction saved = this.transactionRepository.save(transaction);

        return transactionMapper.toDTO(saved);
    }

    /**
     * Dashboard ledger for the given month: standalone transactions (not tied to a bill) plus one aggregated
     * entry per credit-card bill due in the month. Defaults to the current month when year/month are null.
     */
    public List<LedgerEntryDTO> getByAuthenticatedUser(Integer year, Integer month) {
        User authenticatedUser = this.authService.getAuthenticatedUser();

        YearMonth period = resolvePeriod(year, month);
        LocalDate start = period.atDay(1);
        LocalDate end = period.atEndOfMonth();

        List<LedgerEntryDTO> entries = new ArrayList<>();

        transactionRepository
                .findVisibleLedgerEntries(authenticatedUser.getId(), start, end,
                        PluggyCategory.hiddenLedgerLabels(), AccountType.CARTAO_CREDITO)
                .forEach(t -> entries.add(LedgerEntryDTO.of(transactionMapper.toDTO(t))));

        for (BillResponseDTO bill : billService.getBillsForPeriod(authenticatedUser.getId(), start, end)) {
            entries.add(LedgerEntryDTO.of(bill));
        }

        entries.sort(Comparator.comparing(LedgerEntryDTO::getDate).reversed());
        return entries;
    }

    public TransactionResponseDTO update(Long id, TransactionRequestDTO requestDTO) {
        User authenticatedUser = this.authService.getAuthenticatedUser();

        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Transação não encontrada"));
        if (!transaction.getUser().getId().equals(authenticatedUser.getId())) {
            throw new ForbiddenException("Acesso negado");
        }

        if (transaction.isImported()) {
            ensureOnlyDescriptionChanged(transaction, requestDTO);
            transaction.setDescription(requestDTO.getDescription());
        } else {
            Category category = this.categoryService.getIfAvailableForUser(requestDTO.getCategoryId(),
                    authenticatedUser.getId());
            transaction.setAmount(requestDTO.getAmount());
            transaction.setType(requestDTO.getType());
            transaction.setDescription(requestDTO.getDescription());
            transaction.setDate(requestDTO.getDate());
            transaction.setCategory(category);
        }

        Transaction saved = this.transactionRepository.save(transaction);
        return transactionMapper.toDTO(saved);
    }

    private void ensureOnlyDescriptionChanged(Transaction transaction, TransactionRequestDTO requestDTO) {
        Long currentCategoryId = transaction.getCategory() != null ? transaction.getCategory().getId() : null;

        boolean amountChanged = requestDTO.getAmount() == null
                || transaction.getAmount().compareTo(requestDTO.getAmount()) != 0;
        boolean typeChanged = transaction.getType() != requestDTO.getType();
        boolean dateChanged = !transaction.getDate().equals(requestDTO.getDate());
        boolean categoryChanged = !Objects.equals(currentCategoryId, requestDTO.getCategoryId());

        if (amountChanged || typeChanged || dateChanged || categoryChanged) {
            throw new ConflictException("Transações importadas só permitem editar a descrição");
        }
    }

    private YearMonth resolvePeriod(Integer year, Integer month) {
        YearMonth current = YearMonth.now();
        int resolvedYear = year != null ? year : current.getYear();
        int resolvedMonth = month != null ? month : current.getMonthValue();
        return YearMonth.of(resolvedYear, resolvedMonth);
    }

    public void delete(Long id) {
        User authenticatedUser = this.authService.getAuthenticatedUser();
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Transação não encontrada"));
        if (!transaction.getUser().getId().equals(authenticatedUser.getId())) {
            throw new ForbiddenException("Acesso negado");
        }
        if (transaction.getBill() != null) {
            throw new ConflictException("Transações de fatura não podem ser removidas");
        }
        transactionRepository.delete(transaction);
    }
}
