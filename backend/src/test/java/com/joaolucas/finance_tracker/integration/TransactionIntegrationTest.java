package com.joaolucas.finance_tracker.integration;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.joaolucas.finance_tracker.dto.transaction.TransactionRequestDTO;
import com.joaolucas.finance_tracker.entity.Category;
import com.joaolucas.finance_tracker.entity.Transaction;
import com.joaolucas.finance_tracker.entity.TransactionType;
import com.joaolucas.finance_tracker.entity.User;
import com.joaolucas.finance_tracker.repository.CategoryRepository;
import com.joaolucas.finance_tracker.repository.TransactionRepository;
import com.joaolucas.finance_tracker.repository.UserRepository;
import com.joaolucas.finance_tracker.security.JwtService;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import tools.jackson.databind.ObjectMapper;


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

    @Value ("${jwt.secret}")
    private String secret;
    private User user;
    private Category category;
    private LocalDate today;

    @BeforeEach
    void setup() {
        this.user = createUser("John", "john@email.com");
        this.category = createCategory(user, "Food");
        today = LocalDate.now();
    }

    @Nested
    class CreateTransaction {

        @Test
        void shouldCreateTransaction_whenValidRequest() throws Exception {
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
                    .andExpect(jsonPath("$.date").value(today.toString()))
                    .andExpect(jsonPath("$.category.id").value(category.getId()))
                    .andExpect(jsonPath("$.category.name").value("Food"));

            List<Transaction> persistedTransactions = transactionRepository.findAll();
            assertEquals(1, persistedTransactions.size());

            Transaction persistedTransaction = persistedTransactions.get(0);
            assertEquals(DEFAULT_AMOUNT, persistedTransaction.getAmount());
            assertEquals(TransactionType.EXPENSE, persistedTransaction.getType());
            assertEquals(today, persistedTransaction.getDate());
            assertEquals(category.getId(), persistedTransaction.getCategory().getId());
            assertEquals(category.getName(), persistedTransaction.getCategory().getName());
            assertEquals(user, persistedTransaction.getUser());
        }

        @Test
        void shouldReturn401_whenMissingAuthorizationHeader() throws Exception {
            TransactionRequestDTO request = createValidTransactionRequest(category.getId());

            // when + then
            mockMvc.perform(post("/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.error").value("Unauthorized"))
                    .andExpect(jsonPath("$.message").value("Token não fornecido"))
                    .andExpect(jsonPath("$.path").value("/transactions"));
        }

        @Test
        void shouldReturn401_whenInvalidToken() throws Exception {
            TransactionRequestDTO request = createValidTransactionRequest(category.getId());

            // when + then
            mockMvc.perform(post("/transactions")
                            .header("Authorization", "Bearer invalidToken")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.error").value("Unauthorized"))
                    .andExpect(jsonPath("$.message").value("Token inválido"))
                    .andExpect(jsonPath("$.path").value("/transactions"));
        }

        @Test
        void shouldReturn401_whenExpiredToken() throws Exception {
            TransactionRequestDTO request = createValidTransactionRequest(category.getId());

            mockMvc.perform(post("/transactions")
                            .header("Authorization", "Bearer " + expiredToken(user.getId()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.error").value("Unauthorized"))
                    .andExpect(jsonPath("$.message").value("Token expirado"))
                    .andExpect(jsonPath("$.path").value("/transactions"));
        }

        @Test
        void shouldReturn403_whenCategoryDoesNotBelongToUser() throws Exception {
            User otherUser = userRepository.save(
                    User.builder()
                            .name("Mary")
                            .email("mary@email.com")
                            .password("maryStrongPassword123")
                            .build()
            );

            TransactionRequestDTO request = createValidTransactionRequest(category.getId());

            // when + then
            mockMvc.perform(post("/transactions")
                            .header("Authorization", bearerToken(otherUser.getId()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.error").value("Forbidden"))
                    .andExpect(jsonPath("$.message").value("Categoria inválida para este usuário"))
                    .andExpect(jsonPath("$.path").value("/transactions"));
        }

        @Test
        void shouldReturn404_whenCategoryDoesNotExist() throws Exception {
            TransactionRequestDTO request = createValidTransactionRequest(999L);

            // when + then
            mockMvc.perform(post("/transactions")
                            .header("Authorization", bearerToken(user.getId()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.error").value("Not Found"))
                    .andExpect(jsonPath("$.message").value("Categoria não encontrada"))
                    .andExpect(jsonPath("$.path").value("/transactions"));
        }

        @Test
        void shouldReturn400_whenInvalidDataInRequest() throws Exception {
            TransactionRequestDTO request = new TransactionRequestDTO();
            request.setType(null);
            request.setDate(null);
            request.setAmount(BigDecimal.valueOf(-1));

            mockMvc.perform(post("/transactions")
                            .header("Authorization", bearerToken(user.getId()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.error").value("Bad Request"))
                    .andExpect(jsonPath("$.message").isNotEmpty())
                    .andExpect(jsonPath("$.path").value("/transactions"))
                    .andExpect(jsonPath("$.errors").isArray());
        }
    }

    @Nested
    class GetTransactions {

        @Test
        void shouldReturnTransactions_whenUserHasTransactions() throws Exception {
            transactionRepository.saveAll(List.of(
                    Transaction.builder()
                            .amount(DEFAULT_AMOUNT)
                            .type(TransactionType.EXPENSE)
                            .date(today)
                            .description("Transaction 1")
                            .category(category)
                            .user(user)
                            .build(),
                    Transaction.builder()
                            .amount(BigDecimal.valueOf(50))
                            .type(TransactionType.INCOME)
                            .date(today)
                            .description("Transaction 2")
                            .category(category)
                            .user(user)
                            .build()
            ));

            mockMvc.perform(get("/transactions")
                            .header("Authorization", bearerToken(user.getId())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].description").value("Transaction 1"))
                    .andExpect(jsonPath("$[1].description").value("Transaction 2"));
        }

        @Test
        void shouldReturnEmptyList_whenUserHasNoTransactions() throws Exception {
            mockMvc.perform(get("/transactions")
                            .header("Authorization", bearerToken(user.getId())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        void shouldNotReturnTransactionsFromOtherUsers() throws Exception {
            User otherUser = createUser("Mary", "mary@email.com");

            transactionRepository.save(
                    Transaction.builder()
                            .amount(DEFAULT_AMOUNT)
                            .type(TransactionType.EXPENSE)
                            .date(today)
                            .description("Other user transaction")
                            .category(category)
                            .user(otherUser)
                            .build()
            );

            mockMvc.perform(get("/transactions")
                            .header("Authorization", bearerToken(user.getId())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        void shouldReturn401_whenMissingAuthorizationHeader() throws Exception {
            mockMvc.perform(get("/transactions"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.error").value("Unauthorized"))
                    .andExpect(jsonPath("$.message").value("Token não fornecido"))
                    .andExpect(jsonPath("$.path").value("/transactions"));
        }

        @Test
        void shouldReturn401_whenInvalidToken() throws Exception {
            mockMvc.perform(get("/transactions")
                            .header("Authorization", "Bearer invalidToken"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.error").value("Unauthorized"))
                    .andExpect(jsonPath("$.message").value("Token inválido"))
                    .andExpect(jsonPath("$.path").value("/transactions"));
        }

        @Test
        void shouldReturn401_whenExpiredToken() throws Exception {
            TransactionRequestDTO request = createValidTransactionRequest(category.getId());

            mockMvc.perform(get("/transactions")
                            .header("Authorization", "Bearer " + expiredToken(user.getId()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.error").value("Unauthorized"))
                    .andExpect(jsonPath("$.message").value("Token expirado"))
                    .andExpect(jsonPath("$.path").value("/transactions"));
        }
    }

    private TransactionRequestDTO createValidTransactionRequest(Long categoryId) {
        TransactionRequestDTO request = new TransactionRequestDTO();
        request.setAmount(DEFAULT_AMOUNT);
        request.setType(TransactionType.EXPENSE);
        request.setDate(today);
        request.setDescription("Detailed description");
        request.setCategoryId(categoryId);
        return request;
    }

    private String bearerToken(Long userId) {
        return "Bearer " + this.jwtService.generateToken(userId);
    }

    private String expiredToken(Long userId) {
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .setIssuedAt(new Date(System.currentTimeMillis() - 60000))
                .setExpiration(new Date(System.currentTimeMillis() - 1000))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    private User createUser(String name, String email) {
        return userRepository.save(User.builder()
                .name(name)
                .email(email)
                .password("password")
                .build());
    }

    private Category createCategory(User user, String name) {
        return categoryRepository.save(
                Category.builder()
                        .name(name)
                        .user(user)
                        .isDefault(false)
                        .build()
        );
    }
}