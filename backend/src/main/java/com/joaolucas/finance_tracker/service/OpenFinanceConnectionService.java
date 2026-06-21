package com.joaolucas.finance_tracker.service;

import com.joaolucas.finance_tracker.dto.openfinance.*;
import com.joaolucas.finance_tracker.entity.ConnectionStatus;
import com.joaolucas.finance_tracker.entity.FinancialConnection;
import com.joaolucas.finance_tracker.entity.User;
import com.joaolucas.finance_tracker.exception.ForbiddenException;
import com.joaolucas.finance_tracker.exception.NotFoundException;
import com.joaolucas.finance_tracker.mapper.FinancialAccountMapper;
import com.joaolucas.finance_tracker.mapper.FinancialConnectionMapper;
import com.joaolucas.finance_tracker.openfinance.OpenFinanceProvider;
import com.joaolucas.finance_tracker.repository.FinancialAccountRepository;
import com.joaolucas.finance_tracker.repository.FinancialConnectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OpenFinanceConnectionService {

    private final AuthService authService;
    private final OpenFinanceProvider provider;
    private final OpenFinanceSyncService syncService;
    private final FinancialConnectionRepository connectionRepository;
    private final FinancialAccountRepository accountRepository;
    private final FinancialConnectionMapper connectionMapper;
    private final FinancialAccountMapper accountMapper;

    public OpenFinanceConnectionService(AuthService authService,
                                        OpenFinanceProvider provider,
                                        OpenFinanceSyncService syncService,
                                        FinancialConnectionRepository connectionRepository,
                                        FinancialAccountRepository accountRepository,
                                        FinancialConnectionMapper connectionMapper,
                                        FinancialAccountMapper accountMapper) {
        this.authService = authService;
        this.provider = provider;
        this.syncService = syncService;
        this.connectionRepository = connectionRepository;
        this.accountRepository = accountRepository;
        this.connectionMapper = connectionMapper;
        this.accountMapper = accountMapper;
    }

    public ConnectTokenResponseDTO generateConnectToken() {
        Long userId = authService.getAuthenticatedUserId();
        String token = provider.generateConnectToken("user-" + userId);
        return new ConnectTokenResponseDTO(token);
    }

    @Transactional
    public FinancialConnectionResponseDTO registerConnection(RegisterConnectionRequestDTO request) {
        User user = authService.getAuthenticatedUser();

        FinancialConnection connection = FinancialConnection.builder()
                .user(user)
                .institutionName(request.getInstitutionName())
                .provider(provider.getProviderName())
                .externalItemId(request.getItemId())
                .status(ConnectionStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        connection = connectionRepository.save(connection);

        // Initial sync: last 90 days
        syncService.syncConnection(connection, LocalDate.now().minusDays(90));

        return connectionMapper.toDTO(connection);
    }

    public List<FinancialConnectionResponseDTO> listConnections() {
        Long userId = authService.getAuthenticatedUserId();
        return connectionRepository.findByUserId(userId).stream()
                .map(connectionMapper::toDTO)
                .toList();
    }

    @Transactional
    public void disconnectConnection(Long id) {
        User user = authService.getAuthenticatedUser();
        FinancialConnection connection = connectionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Conexão não encontrada"));
        if (!connection.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Acesso negado");
        }
        connection.setStatus(ConnectionStatus.DISCONNECTED);
        connection.setUpdatedAt(LocalDateTime.now());
        connectionRepository.save(connection);
    }

    @Transactional
    public void syncManual(Long id) {
        User user = authService.getAuthenticatedUser();
        FinancialConnection connection = connectionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Conexão não encontrada"));
        if (!connection.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Acesso negado");
        }
        // Daily syncs fetch last 2 days to catch any late-arriving transactions
        syncService.syncConnection(connection, LocalDate.now().minusDays(2));
    }

    public List<FinancialAccountResponseDTO> listAccounts() {
        Long userId = authService.getAuthenticatedUserId();
        return accountRepository.findByUserId(userId).stream()
                .map(accountMapper::toDTO)
                .toList();
    }
}
