package com.fudn.customerservice;

import com.fudn.customerservice.config.PasswordConfig;
import com.fudn.customerservice.controller.AuthController;
import com.fudn.customerservice.dto.LoginRequest;
import com.fudn.customerservice.dto.LoginResponse;
import com.fudn.customerservice.exception.ApiException;
import com.fudn.customerservice.exception.GlobalExceptionHandler;
import com.fudn.customerservice.model.Customer;
import com.fudn.customerservice.model.CustomerStatus;
import com.fudn.customerservice.repository.CustomerRepository;
import com.fudn.customerservice.security.JwtService;
import com.fudn.customerservice.service.AuthService;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CustomerServiceApplicationTests {

    @Mock
    private CustomerRepository customerRepository;

    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private AuthService authService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        passwordEncoder = new PasswordConfig().passwordEncoder();
        jwtService = new JwtService("fu-cinema-booking-system-secret-key-2026-mss301", 60);
        authService = new AuthService(customerRepository, passwordEncoder, jwtService);
        ReflectionTestUtils.setField(authService, "adminEmail", "admin@fucinema.com");
        ReflectionTestUtils.setField(authService, "adminPassword", "admin123");

        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void contextLoads() {
    }

    @Test
    void flyway_migration_shouldMigrateSuccessfully() {
        Flyway flyway = Flyway.configure()
                .dataSource("jdbc:sqlserver://localhost:1433;databaseName=cinema_customer;encrypt=true;trustServerCertificate=true",
                        "sa", "Fucinema@2026")
                .locations("classpath:db/migration")
                .load();
        MigrateResult result = flyway.migrate();
        assertTrue(result.success);
    }

    @Test
    void customerEntity_shouldMapCorrectly() {
        Customer customer = Customer.builder()
                .customerId(1L)
                .customerName("Nguyễn Văn An")
                .telephone("0905123456")
                .email("an@gmail.com")
                .customerBirthday(LocalDate.of(2002, 5, 10))
                .customerStatus(CustomerStatus.ACTIVE)
                .password("$2a$10$dmoDdVpWYdqLarqBfkYQteoq1YORLC5LLMd55bpomZ3EarS/vtjtW")
                .build();

        assertEquals(1L, customer.getCustomerId());
        assertEquals("Nguyễn Văn An", customer.getCustomerName());
        assertEquals("0905123456", customer.getTelephone());
        assertEquals("an@gmail.com", customer.getEmail());
        assertEquals(LocalDate.of(2002, 5, 10), customer.getCustomerBirthday());
        assertEquals(CustomerStatus.ACTIVE, customer.getCustomerStatus());
        assertEquals("$2a$10$dmoDdVpWYdqLarqBfkYQteoq1YORLC5LLMd55bpomZ3EarS/vtjtW", customer.getPassword());
    }

    @Test
    void passwordEncoder_shouldEncodeAndMatch() {
        String raw = "samplePassword123";
        String encoded = passwordEncoder.encode(raw);

        assertNotEquals(raw, encoded);
        assertTrue(passwordEncoder.matches(raw, encoded));
    }

    @Test
    void jwtService_shouldGenerateAndDecodeToken() {
        String token = jwtService.generateToken(1L, "an@gmail.com", "CUSTOMER");
        assertNotNull(token);
        assertFalse(token.isBlank());

        Jwt decoded = jwtService.decodeToken(token);
        assertEquals("an@gmail.com", decoded.getSubject());
        assertEquals(Long.valueOf(1L), decoded.<Long>getClaim("uid"));
        assertEquals("CUSTOMER", decoded.getClaim("role"));
        assertEquals("fu-cinema", decoded.getClaimAsString("iss"));
    }

    @Test
    void authService_adminLogin_success() {
        LoginResponse response = authService.login(new LoginRequest("admin@fucinema.com", "admin123"));
        assertEquals(0L, response.userId());
        assertEquals("ADMIN", response.role());
        assertEquals("admin@fucinema.com", response.email());
        assertNotNull(response.accessToken());
    }

    @Test
    void authService_adminLogin_wrongPassword() {
        ApiException ex = assertThrows(ApiException.class, () ->
                authService.login(new LoginRequest("admin@fucinema.com", "wrongpass")));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }

    @Test
    void authService_customerLogin_success() {
        Customer customer = Customer.builder()
                .customerId(10L)
                .customerName("John Doe")
                .email("john@example.com")
                .password(passwordEncoder.encode("secret123"))
                .customerStatus(CustomerStatus.ACTIVE)
                .build();
        when(customerRepository.findByEmailIgnoreCase("john@example.com")).thenReturn(Optional.of(customer));

        LoginResponse response = authService.login(new LoginRequest("john@example.com", "secret123"));
        assertEquals(10L, response.userId());
        assertEquals("CUSTOMER", response.role());
        assertEquals("john@example.com", response.email());
        assertEquals("John Doe", response.fullName());
    }

    @Test
    void authService_customerLogin_inactiveAccount() {
        Customer customer = Customer.builder()
                .customerId(11L)
                .customerName("Inactive User")
                .email("inactive@example.com")
                .password(passwordEncoder.encode("secret123"))
                .customerStatus(CustomerStatus.INACTIVE)
                .build();
        when(customerRepository.findByEmailIgnoreCase("inactive@example.com")).thenReturn(Optional.of(customer));

        ApiException ex = assertThrows(ApiException.class, () ->
                authService.login(new LoginRequest("inactive@example.com", "secret123")));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    void authService_customerLogin_wrongPassword() {
        Customer customer = Customer.builder()
                .customerId(12L)
                .customerName("Test User")
                .email("test@example.com")
                .password(passwordEncoder.encode("correctpass"))
                .customerStatus(CustomerStatus.ACTIVE)
                .build();
        when(customerRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(customer));

        ApiException ex = assertThrows(ApiException.class, () ->
                authService.login(new LoginRequest("test@example.com", "wrongpass")));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }

    @Test
    void authController_login_success() throws Exception {
        String json = "{\"email\":\"admin@fucinema.com\",\"password\":\"admin123\"}";
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin@fucinema.com"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    void authController_login_invalidCredentials_returns401() throws Exception {
        String json = "{\"email\":\"admin@fucinema.com\",\"password\":\"wrongpassword\"}";
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void authController_login_validationFailure_returns400() throws Exception {
        String json = "{\"email\":\"\",\"password\":\"\"}";
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }
}