package com.joaolucas.finance_tracker.controller;


import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.joaolucas.finance_tracker.config.BaseControllerTest;
import com.joaolucas.finance_tracker.dto.transaction.TransactionRequestDTO;
import com.joaolucas.finance_tracker.dto.transaction.TransactionResponseDTO;
import com.joaolucas.finance_tracker.entity.TransactionType;
import com.joaolucas.finance_tracker.exception.ForbiddenException;
import com.joaolucas.finance_tracker.exception.GlobalExceptionHandler;
import com.joaolucas.finance_tracker.security.JwtService;
import com.joaolucas.finance_tracker.service.TransactionService;


@WebMvcTest (TransactionController.class)
@Import ({ GlobalExceptionHandler.class, JacksonAutoConfiguration.class })
@AutoConfigureMockMvc (addFilters = false)
class TransactionControllerTest extends BaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionService transactionService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void create$shouldReturn200WhenSuccess() throws Exception {
        TransactionRequestDTO request = getValidRequestDTO();

        TransactionResponseDTO response = new TransactionResponseDTO();

        when(transactionService.create(any())).thenReturn(response);

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isCreated());

        verify(transactionService).create(any());
    }

    @Test
    void create$shouldReturn403WhenCategoryIsInvalid() throws Exception {
        TransactionRequestDTO request = getValidRequestDTO();

        when(transactionService.create(any()))
                .thenThrow(new ForbiddenException("Categoria inválida para este usuário"));

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Categoria inválida para este usuário"))
                .andExpect(jsonPath("$.path").value("/transactions"));
    }

    @Test
    void create$shouldReturn500WhenUnexpectedErrorOccurs() throws Exception {
        TransactionRequestDTO request = getValidRequestDTO();

        when(transactionService.create(any()))
                .thenThrow(new RuntimeException("Erro inesperado"));

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("Erro inesperado"));
    }

    @Test
    void create$shouldReturn400WhenValidationFails() throws Exception {
        TransactionRequestDTO request = new TransactionRequestDTO();

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void getAllFromAuthenticatedUser$shouldReturn200WithList() throws Exception {
        when(transactionService.getByAuthenticatedUser())
                .thenReturn(List.of(new TransactionResponseDTO()));

        mockMvc.perform(get("/transactions"))
                .andExpect(status().isOk());

        verify(transactionService).getByAuthenticatedUser();
    }

    @Test
    void getAllFromAuthenticatedUser$shouldReturn200WithEmptyList() throws Exception {
        when(transactionService.getByAuthenticatedUser())
                .thenReturn(List.of());

        mockMvc.perform(get("/transactions"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    private static TransactionRequestDTO getValidRequestDTO() {
        TransactionRequestDTO request = new TransactionRequestDTO();
        request.setCategoryId(10L);
        request.setDate(LocalDate.now());
        request.setAmount(BigDecimal.valueOf(100.0));
        request.setType(TransactionType.INCOME);
        return request;
    }
}