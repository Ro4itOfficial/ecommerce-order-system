package com.ecommerce.order.integration;

import com.ecommerce.order.model.dto.request.CreateOrderRequest;
import com.ecommerce.order.model.dto.request.LoginRequest;
import com.ecommerce.order.model.dto.response.JwtResponse;
import com.ecommerce.order.model.entity.User;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.order.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
class OrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String accessToken;
    private User testUser;

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @BeforeEach
    void setUp() throws Exception {
        // Clean up
        orderRepository.deleteAll();
        userRepository.deleteAll();

        // Create test user
        testUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .password(passwordEncoder.encode("Password123!"))
                .firstName("Test")
                .lastName("User")
                .roles(Set.of("USER"))
                .enabled(true)
                .emailVerified(true)
                .build();
        userRepository.save(testUser);

        // Login and get token
        LoginRequest loginRequest = LoginRequest.builder()
                .username("testuser")
                .password("Password123!")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JwtResponse jwtResponse = objectMapper.readValue(
                loginResult.getResponse().getContentAsString(),
                JwtResponse.class
        );
        accessToken = jwtResponse.getAccessToken();
    }

    @Test
    void createOrder_WithValidData_ShouldCreateSuccessfully() throws Exception {
        // Given
        CreateOrderRequest.OrderItemRequest item = CreateOrderRequest.OrderItemRequest.builder()
                .productId("PROD001")
                .productName("Test Product")
                .quantity(2)
                .unitPrice(new BigDecimal("99.99"))
                .build();

        CreateOrderRequest request = CreateOrderRequest.builder()
                .customerId("CUST001")
                .customerEmail("customer@example.com")
                .customerName("Test Customer")
                .items(List.of(item))
                .shippingAddress("123 Test St")
                .paymentMethod("CREDIT_CARD")
                .build();

        // When & Then
        MvcResult result = mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId").value("CUST001"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].productName").value("Test Product"))
                .andReturn();

        // Verify
        String responseBody = result.getResponse().getContentAsString();
        assertThat(responseBody).isNotEmpty();
        assertThat(orderRepository.count()).isEqualTo(1);
    }

    @Test
    void createOrder_WithoutAuthentication_ShouldReturn401() throws Exception {
        // Given
        CreateOrderRequest request = CreateOrderRequest.builder()
                .customerId("CUST001")
                .items(List.of())
                .build();

        // When & Then
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createOrder_WithInvalidData_ShouldReturn400() throws Exception {
        // Given - Order without required fields
        CreateOrderRequest request = CreateOrderRequest.builder()
                .customerId("") // Invalid: empty customer ID
                .items(List.of()) // Invalid: empty items
                .build();

        // When & Then
        mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void getOrder_WithValidId_ShouldReturnOrder() throws Exception {
        // Given - Create an order first
        CreateOrderRequest.OrderItemRequest item = CreateOrderRequest.OrderItemRequest.builder()
                .productId("PROD001")
                .productName("Test Product")
                .quantity(1)
                .unitPrice(new BigDecimal("49.99"))
                .build();

        CreateOrderRequest request = CreateOrderRequest.builder()
                .customerId("CUST001")
                .customerEmail("customer@example.com")
                .customerName("Test Customer")
                .items(List.of(item))
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String orderId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("orderId").asText();

        // When & Then
        mockMvc.perform(get("/api/v1/orders/{orderId}", orderId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.customerId").value("CUST001"));
    }

    @Test
    void cancelOrder_WithValidId_ShouldCancelSuccessfully() throws Exception {
        // Given - Create an order first
        CreateOrderRequest.OrderItemRequest item = CreateOrderRequest.OrderItemRequest.builder()
                .productId("PROD001")
                .productName("Test Product")
                .quantity(1)
                .unitPrice(new BigDecimal("99.99"))
                .build();

        CreateOrderRequest request = CreateOrderRequest.builder()
                .customerId("CUST001")
                .items(List.of(item))
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String orderId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("orderId").asText();

        // When & Then
        mockMvc.perform(post("/api/v1/orders/{orderId}/cancel", orderId)
                        .header("Authorization", "Bearer " + accessToken)
                        .param("reason", "Customer request"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancelledReason").value("Customer request"));
    }

    @Test
    void searchOrders_WithFilters_ShouldReturnFilteredResults() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/orders/search")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("customerId", "CUST001")
                        .param("status", "PENDING")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.pageable").exists());
    }
}