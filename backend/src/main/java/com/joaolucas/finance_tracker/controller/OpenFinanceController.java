package com.joaolucas.finance_tracker.controller;

import com.joaolucas.finance_tracker.dto.openfinance.*;
import com.joaolucas.finance_tracker.service.OpenFinanceConnectionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/open-finance")
public class OpenFinanceController {

    private final OpenFinanceConnectionService connectionService;

    public OpenFinanceController(OpenFinanceConnectionService connectionService) {
        this.connectionService = connectionService;
    }

    @PostMapping("/connect-token")
    public ConnectTokenResponseDTO generateConnectToken() {
        return connectionService.generateConnectToken();
    }

    @PostMapping("/connections")
    @ResponseStatus(HttpStatus.CREATED)
    public FinancialConnectionResponseDTO registerConnection(@RequestBody @Valid RegisterConnectionRequestDTO request) {
        return connectionService.registerConnection(request);
    }

    @GetMapping("/connections")
    public List<FinancialConnectionResponseDTO> listConnections() {
        return connectionService.listConnections();
    }

    @DeleteMapping("/connections/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void disconnectConnection(@PathVariable Long id) {
        connectionService.disconnectConnection(id);
    }

    @PostMapping("/connections/{id}/sync")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void syncManual(@PathVariable Long id) {
        connectionService.syncManual(id);
    }

    @GetMapping("/accounts")
    public List<FinancialAccountResponseDTO> listAccounts() {
        return connectionService.listAccounts();
    }
}
