package com.joaolucas.finance_tracker.controller;


import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.joaolucas.finance_tracker.dto.transaction.TransactionRequestDTO;
import com.joaolucas.finance_tracker.dto.transaction.TransactionResponseDTO;
import com.joaolucas.finance_tracker.service.TransactionService;

import jakarta.validation.Valid;


@RestController
@RequestMapping ("/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    @ResponseStatus (HttpStatus.CREATED)
    public TransactionResponseDTO create(@RequestBody @Valid TransactionRequestDTO transaction) {
        return transactionService.create(transaction);
    }

    @GetMapping
    public List<TransactionResponseDTO> getAllFromAuthenticadeUser() {
        return transactionService.getByAuthenticatedUser();
    }
}
