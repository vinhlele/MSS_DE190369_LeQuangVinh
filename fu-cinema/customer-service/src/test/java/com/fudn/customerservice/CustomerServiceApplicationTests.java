package com.fudn.customerservice;

import com.fudn.customerservice.config.PasswordConfig;
import com.fudn.customerservice.controller.AuthController;
import com.fudn.customerservice.controller.CustomerController;
import com.fudn.customerservice.dto.*;
import com.fudn.customerservice.exception.ApiException;
import com.fudn.customerservice.exception.GlobalExceptionHandler;
import com.fudn.customerservice.model.Customer;
import com.fudn.customerservice.model.CustomerStatus;
import com.fudn.customerservice.repository.CustomerRepository;
import com.fudn.customerservice.security.JwtService;
import com.fudn.customerservice.service.AuthService;
import com.fudn.customerservice.service.CustomerService;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CustomerServiceApplicationTests {

    @Mock
    private CustomerRepository customerRepository;

    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private AuthService authService;
    private CustomerService customerService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        passwordEncoder = new PasswordConfig().passwordEncoder();
        jwtService = new JwtService("fu-cinema-booking-system-secret-key-2026-mss301", 60);
        authService = new AuthService(customerRepository, passwordEncoder, jwtService);
        ReflectionTestUtils.setField(authService, "adminEmail", "admin@fucinema.com");
        ReflectionTestUtils.setField(authService, "adminPassword", "admin123");

        customerService = new CustomerService(customerRepository, passwordEncoder);
        ReflectionTestUtils.setField(customerService, "adminEmail", "admin@fucinema.com");

        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService), new CustomerController(customerService))
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
    void customerService_register_success() {
        RegisterRequest req = new RegisterRequest(
                "Đỗ Nam Trung",
                "0909999888",
                "donamtrung@gmail.com",
                LocalDate.of(1995, 6, 15),
                "securepass123");

        when(customerRepository.existsByEmailIgnoreCase("donamtrung@gmail.com")).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> {
            Customer c = invocation.getArgument(0);
            c.setCustomerId(50L);
            return c;
        });

        CustomerResponse res = customerService.register(req);
        assertEquals(50L, res.customerId());
        assertEquals("Đỗ Nam Trung", res.customerName());
        assertEquals("donamtrung@gmail.com", res.email());
        assertEquals("0909999888", res.telephone());
        assertEquals(CustomerStatus.ACTIVE, res.customerStatus());
    }

    @Test
    void customerService_register_duplicateEmail_throwsConflict() {
        RegisterRequest req = new RegisterRequest(
                "Trùng Email",
                "0901234567",
                "an@gmail.com",
                LocalDate.of(2000, 1, 1),
                "password123");

        when(customerRepository.existsByEmailIgnoreCase("an@gmail.com")).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class, () -> customerService.register(req));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertTrue(ex.getMessage().contains("Email is already in use"));
    }

    @Test
    void customerService_register_adminEmail_throwsConflict() {
        RegisterRequest req = new RegisterRequest(
                "Fake Admin",
                "0901234567",
                "admin@fucinema.com",
                LocalDate.of(2000, 1, 1),
                "password123");

        ApiException ex = assertThrows(ApiException.class, () -> customerService.register(req));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertTrue(ex.getMessage().contains("Email is already in use"));
    }

    @Test
    void customerService_getProfile_success() {
        Customer customer = Customer.builder()
                .customerId(1L)
                .customerName("Nguyễn Văn An")
                .telephone("0905123456")
                .email("an@gmail.com")
                .customerBirthday(LocalDate.of(2002, 5, 10))
                .customerStatus(CustomerStatus.ACTIVE)
                .password("encoded_pass")
                .build();
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        CustomerResponse res = customerService.getProfile(1L);
        assertEquals(1L, res.customerId());
        assertEquals("Nguyễn Văn An", res.customerName());
        assertEquals("an@gmail.com", res.email());
    }

    @Test
    void customerService_getProfile_notFound() {
        when(customerRepository.findById(999L)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> customerService.getProfile(999L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void customerService_updateProfile_success() {
        Customer customer = Customer.builder()
                .customerId(2L)
                .customerName("Trần Thị Bình")
                .telephone("0914234567")
                .email("binh@gmail.com")
                .customerBirthday(LocalDate.of(2003, 8, 21))
                .customerStatus(CustomerStatus.ACTIVE)
                .build();
        when(customerRepository.findById(2L)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProfileUpdateRequest updateReq = new ProfileUpdateRequest(
                "Trần Thị Bình Updated",
                "0919999999",
                LocalDate.of(2003, 8, 22));

        CustomerResponse res = customerService.updateProfile(2L, updateReq);
        assertEquals("Trần Thị Bình Updated", res.customerName());
        assertEquals("0919999999", res.telephone());
        assertEquals(LocalDate.of(2003, 8, 22), res.customerBirthday());
    }

    @Test
    void customerService_changePassword_success() {
        String oldRaw = "oldPass123";
        String newRaw = "newPass456";
        Customer customer = Customer.builder()
                .customerId(1L)
                .customerName("Nguyễn Văn An")
                .email("an@gmail.com")
                .password(passwordEncoder.encode(oldRaw))
                .build();
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        customerService.changePassword(1L, new ChangePasswordRequest(oldRaw, newRaw));

        assertTrue(passwordEncoder.matches(newRaw, customer.getPassword()));
        verify(customerRepository).save(customer);
    }

    @Test
    void customerService_changePassword_wrongOldPassword() {
        Customer customer = Customer.builder()
                .customerId(1L)
                .password(passwordEncoder.encode("correctOld"))
                .build();
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        ApiException ex = assertThrows(ApiException.class, () ->
                customerService.changePassword(1L, new ChangePasswordRequest("wrongOld", "newPass456")));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("Old password is incorrect", ex.getMessage());
    }

    @Test
    void customerService_changePassword_samePassword() {
        Customer customer = Customer.builder()
                .customerId(1L)
                .password(passwordEncoder.encode("samePass123"))
                .build();
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        ApiException ex = assertThrows(ApiException.class, () ->
                customerService.changePassword(1L, new ChangePasswordRequest("samePass123", "samePass123")));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("New password must be different from the old password", ex.getMessage());
    }

    @Test
    void customerService_adminSearch_all() {
        Customer c1 = Customer.builder().customerId(1L).customerName("A").email("a@a.com").build();
        Customer c2 = Customer.builder().customerId(2L).customerName("B").email("b@b.com").build();
        when(customerRepository.findAll(any(Sort.class))).thenReturn(List.of(c1, c2));

        List<CustomerResponse> result = customerService.search("");
        assertEquals(2, result.size());
    }

    @Test
    void customerService_adminSearch_keyword() {
        Customer c1 = Customer.builder().customerId(1L).customerName("Nguyễn Văn An").email("an@gmail.com").build();
        when(customerRepository.findByCustomerNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrderByCustomerIdAsc("an", "an"))
                .thenReturn(List.of(c1));

        List<CustomerResponse> result = customerService.search("an");
        assertEquals(1, result.size());
        assertEquals("Nguyễn Văn An", result.get(0).customerName());
    }

    @Test
    void customerService_adminGetById_success() {
        Customer c = Customer.builder().customerId(1L).customerName("An").email("an@gmail.com").build();
        when(customerRepository.findById(1L)).thenReturn(Optional.of(c));

        CustomerResponse res = customerService.getById(1L);
        assertEquals(1L, res.customerId());
    }

    @Test
    void customerService_adminCreate_success() {
        AdminCustomerRequest req = new AdminCustomerRequest(
                "Admin Created", "0901234567", "created@gmail.com",
                LocalDate.of(2000, 1, 1), CustomerStatus.ACTIVE, "adminPass123");

        when(customerRepository.existsByEmailIgnoreCase("created@gmail.com")).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> {
            Customer c = invocation.getArgument(0);
            c.setCustomerId(88L);
            return c;
        });

        CustomerResponse res = customerService.create(req);
        assertEquals(88L, res.customerId());
        assertEquals("Admin Created", res.customerName());
        assertEquals("created@gmail.com", res.email());
    }

    @Test
    void customerService_adminCreate_missingPassword_throwsBadRequest() {
        AdminCustomerRequest req = new AdminCustomerRequest(
                "Admin Created", "0901234567", "created@gmail.com",
                LocalDate.of(2000, 1, 1), CustomerStatus.ACTIVE, "");

        ApiException ex = assertThrows(ApiException.class, () -> customerService.create(req));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("Password is required"));
    }

    @Test
    void customerService_adminUpdate_success() {
        Customer existing = Customer.builder().customerId(2L).email("old@gmail.com").password("oldHash").build();
        when(customerRepository.findById(2L)).thenReturn(Optional.of(existing));
        when(customerRepository.existsByEmailIgnoreCaseAndCustomerIdNot("new@gmail.com", 2L)).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminCustomerRequest req = new AdminCustomerRequest(
                "Updated Name", "0911222333", "new@gmail.com",
                LocalDate.of(1999, 1, 1), CustomerStatus.INACTIVE, "newPass789");

        CustomerResponse res = customerService.update(2L, req);
        assertEquals("Updated Name", res.customerName());
        assertEquals("new@gmail.com", res.email());
        assertEquals(CustomerStatus.INACTIVE, res.customerStatus());
        assertTrue(passwordEncoder.matches("newPass789", existing.getPassword()));
    }

    @Test
    void customerService_adminDelete_logicalDeletion_setsInactive() {
        Customer existing = Customer.builder()
                .customerId(1L)
                .customerName("Nguyễn Văn An")
                .email("an@gmail.com")
                .customerStatus(CustomerStatus.ACTIVE)
                .build();
        when(customerRepository.findById(1L)).thenReturn(Optional.of(existing));

        customerService.delete(1L);

        assertEquals(CustomerStatus.INACTIVE, existing.getCustomerStatus());
        verify(customerRepository).save(existing);
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

    @Test
    void customerController_register_success() throws Exception {
        when(customerRepository.existsByEmailIgnoreCase("newcust@gmail.com")).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> {
            Customer c = invocation.getArgument(0);
            c.setCustomerId(99L);
            return c;
        });

        String json = "{\"customerName\":\"Nguyen Van New\",\"telephone\":\"0901234567\",\"email\":\"newcust@gmail.com\",\"customerBirthday\":\"2000-01-01\",\"password\":\"password123\"}";
        mockMvc.perform(post("/api/customers/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId").value(99))
                .andExpect(jsonPath("$.customerName").value("Nguyen Van New"))
                .andExpect(jsonPath("$.email").value("newcust@gmail.com"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void customerController_getProfile_success() throws Exception {
        Customer customer = Customer.builder()
                .customerId(1L)
                .customerName("Nguyễn Văn An")
                .telephone("0905123456")
                .email("an@gmail.com")
                .customerBirthday(LocalDate.of(2002, 5, 10))
                .customerStatus(CustomerStatus.ACTIVE)
                .password("encoded_pass")
                .build();
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        mockMvc.perform(get("/api/customers/me")
                        .header("X-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(1))
                .andExpect(jsonPath("$.customerName").value("Nguyễn Văn An"))
                .andExpect(jsonPath("$.email").value("an@gmail.com"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void customerController_updateProfile_success() throws Exception {
        Customer customer = Customer.builder()
                .customerId(1L)
                .customerName("Nguyễn Văn An")
                .telephone("0905123456")
                .email("an@gmail.com")
                .customerBirthday(LocalDate.of(2002, 5, 10))
                .customerStatus(CustomerStatus.ACTIVE)
                .password("encoded_pass")
                .build();
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String json = "{\"customerName\":\"Nguyen Van An Updated\",\"telephone\":\"0909999888\",\"customerBirthday\":\"2002-05-10\"}";
        mockMvc.perform(put("/api/customers/me")
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerName").value("Nguyen Van An Updated"))
                .andExpect(jsonPath("$.telephone").value("0909999888"));
    }

    @Test
    void customerController_changePassword_success() throws Exception {
        String oldRaw = "oldPass123";
        Customer customer = Customer.builder()
                .customerId(1L)
                .password(passwordEncoder.encode(oldRaw))
                .build();
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        String json = "{\"oldPassword\":\"oldPass123\",\"newPassword\":\"newPass456\"}";
        mockMvc.perform(put("/api/customers/me/password")
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNoContent());
    }

    // TODO 3.3 - Admin Customer Endpoints Tests
    @Test
    void customerController_adminSearch_success() throws Exception {
        Customer c1 = Customer.builder().customerId(1L).customerName("An").email("an@gmail.com").customerStatus(CustomerStatus.ACTIVE).build();
        when(customerRepository.findByCustomerNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrderByCustomerIdAsc("gmail", "gmail"))
                .thenReturn(List.of(c1));

        mockMvc.perform(get("/api/customers").param("keyword", "gmail"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].customerId").value(1))
                .andExpect(jsonPath("$[0].email").value("an@gmail.com"));
    }

    @Test
    void customerController_adminGetById_success() throws Exception {
        Customer c1 = Customer.builder().customerId(1L).customerName("An").email("an@gmail.com").customerStatus(CustomerStatus.ACTIVE).build();
        when(customerRepository.findById(1L)).thenReturn(Optional.of(c1));

        mockMvc.perform(get("/api/customers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(1))
                .andExpect(jsonPath("$.customerName").value("An"));
    }

    @Test
    void customerController_adminCreate_success() throws Exception {
        when(customerRepository.existsByEmailIgnoreCase("new@gmail.com")).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> {
            Customer c = invocation.getArgument(0);
            c.setCustomerId(10L);
            return c;
        });

        String json = "{\"customerName\":\"New User\",\"telephone\":\"0905111222\",\"email\":\"new@gmail.com\",\"customerBirthday\":\"2000-01-01\",\"customerStatus\":\"ACTIVE\",\"password\":\"secret123\"}";
        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId").value(10))
                .andExpect(jsonPath("$.customerName").value("New User"))
                .andExpect(jsonPath("$.email").value("new@gmail.com"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void customerController_adminUpdate_success() throws Exception {
        Customer existing = Customer.builder().customerId(1L).customerName("Old Name").email("old@gmail.com").customerStatus(CustomerStatus.ACTIVE).build();
        when(customerRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(customerRepository.existsByEmailIgnoreCaseAndCustomerIdNot("updated@gmail.com", 1L)).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String json = "{\"customerName\":\"Updated Name\",\"telephone\":\"0905999888\",\"email\":\"updated@gmail.com\",\"customerBirthday\":\"2000-01-01\",\"customerStatus\":\"ACTIVE\"}";
        mockMvc.perform(put("/api/customers/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(1))
                .andExpect(jsonPath("$.customerName").value("Updated Name"))
                .andExpect(jsonPath("$.email").value("updated@gmail.com"));
    }

    @Test
    void customerController_adminDelete_success() throws Exception {
        Customer existing = Customer.builder().customerId(1L).customerName("Name").email("test@gmail.com").customerStatus(CustomerStatus.ACTIVE).build();
        when(customerRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(delete("/api/customers/1"))
                .andExpect(status().isNoContent());

        assertEquals(CustomerStatus.INACTIVE, existing.getCustomerStatus());
    }
}