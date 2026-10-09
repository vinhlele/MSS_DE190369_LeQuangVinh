package com.fudn.customerservice;

import com.fudn.customerservice.config.PasswordConfig;
import com.fudn.customerservice.dto.LoginRequest;
import com.fudn.customerservice.dto.LoginResponse;
import com.fudn.customerservice.exception.ApiException;
import com.fudn.customerservice.model.Customer;
import com.fudn.customerservice.model.CustomerStatus;
import com.fudn.customerservice.repository.CustomerRepository;
import com.fudn.customerservice.security.JwtService;
import com.fudn.customerservice.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceApplicationTests {

    @Mock
    private CustomerRepository customerRepository;

    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new PasswordConfig().passwordEncoder();
        jwtService = new JwtService("fu-cinema-booking-system-secret-key-2026-mss301", 60);
        authService = new AuthService(customerRepository, passwordEncoder, jwtService);
        ReflectionTestUtils.setField(authService, "adminEmail", "admin@fucinema.com");
        ReflectionTestUtils.setField(authService, "adminPassword", "admin123");
    }

    @Test
    void contextLoads() {
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
}