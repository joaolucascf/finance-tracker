package com.joaolucas.finance_tracker.scheduler;

import com.joaolucas.finance_tracker.entity.ConnectionStatus;
import com.joaolucas.finance_tracker.entity.FinancialConnection;
import com.joaolucas.finance_tracker.repository.FinancialConnectionRepository;
import com.joaolucas.finance_tracker.service.OpenFinanceSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class OpenFinanceSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(OpenFinanceSyncScheduler.class);

    private final FinancialConnectionRepository connectionRepository;
    private final OpenFinanceSyncService syncService;

    public OpenFinanceSyncScheduler(FinancialConnectionRepository connectionRepository,
                                    OpenFinanceSyncService syncService) {
        this.connectionRepository = connectionRepository;
        this.syncService = syncService;
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void syncAllActiveConnections() {
        List<FinancialConnection> activeConnections = connectionRepository.findByStatus(ConnectionStatus.ACTIVE);
        log.info("Starting daily sync for {} active connections", activeConnections.size());

        LocalDate from = LocalDate.now().minusDays(2);

        for (FinancialConnection connection : activeConnections) {
            try {
                syncService.syncConnection(connection, from);
            } catch (Exception e) {
                log.error("Unexpected error syncing connection {}: {}", connection.getId(), e.getMessage());
            }
        }

        log.info("Daily sync completed");
    }
}
