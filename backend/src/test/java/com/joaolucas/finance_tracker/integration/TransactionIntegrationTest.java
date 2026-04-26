package com.joaolucas.finance_tracker.integration;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.joaolucas.finance_tracker.dto.transaction.TransactionRequestDTO;
import com.joaolucas.finance_tracker.entity.Category;
import com.joaolucas.finance_tracker.entity.TransactionType;
import com.joaolucas.finance_tracker.entity.User;
import com.joaolucas.finance_tracker.repository.CategoryRepository;
import com.joaolucas.finance_tracker.repository.TransactionRepository;
import com.joaolucas.finance_tracker.repository.UserRepository;
import com.joaolucas.finance_tracker.security.JwtService;


@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles ("test")
@Transactional
class TransactionIntegrationTest {

    private static final BigDecimal DEFAULT_AMOUNT = BigDecimal.valueOf(25.50);

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JwtService jwtService;

    private User user;

    @BeforeEach
    void setup() {
        this.user = userRepository.save(User.builder()
                .name("John")
                .email("john@email.com")
                .password("myStrongPassword123")
                .build()
        );
    }

    @Test
    void shouldCreateTransaction_whenValidRequest() throws Exception {
        Category category = this.createCategory("Food");

        TransactionRequestDTO request = createValidTransactionRequest(category.getId());

        // when + then
        mockMvc.perform(post("/transactions")
                        .header("Authorization", bearerToken(user.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value("Detailed description"))
                .andExpect(jsonPath("$.amount").value(DEFAULT_AMOUNT))
                .andExpect(jsonPath("$.type").value("EXPENSE"))
                .andExpect(jsonPath("$.date").value(LocalDate.now().toString()))
                .andExpect(jsonPath("$.category.id").value(category.getId()))
                .andExpect(jsonPath("$.category.name").value("Food"));

        assertEquals(1, transactionRepository.count());
    }

    private Category createCategory(String name) {
        return this.categoryRepository.save(
                Category.builder()
                        .name(name)
                        .user(user)
                        .isDefault(false)
                        .build()
        );
    }

    private TransactionRequestDTO createValidTransactionRequest(Long categoryId) {
        TransactionRequestDTO request = new TransactionRequestDTO();
        request.setAmount(DEFAULT_AMOUNT);
        request.setType(TransactionType.EXPENSE);
        request.setDate(LocalDate.now());
        request.setDescription("Detailed description");
        request.setCategoryId(categoryId);
        return request;
    }

    private String bearerToken(Long userId) {
        return "Bearer " + this.jwtService.generateToken(userId);
    }
}