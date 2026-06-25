package com.joaolucas.finance_tracker.controller;

import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.joaolucas.finance_tracker.dto.bill.BillRenameRequestDTO;
import com.joaolucas.finance_tracker.dto.bill.BillResponseDTO;
import com.joaolucas.finance_tracker.service.BillService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/bills")
public class BillController {

    private final BillService billService;

    public BillController(BillService billService) {
        this.billService = billService;
    }

    @PatchMapping("/{id}")
    public BillResponseDTO rename(@PathVariable Long id, @RequestBody @Valid BillRenameRequestDTO request) {
        return billService.rename(id, request.getName());
    }
}
