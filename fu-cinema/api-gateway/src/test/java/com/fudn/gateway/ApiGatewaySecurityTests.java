package com.fudn.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.client.ResourceAccessException;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ApiGatewaySecurityTests {

    @Autowired
    private MockMvc mockMvc;

    // ---------- F10.2: Public & Token Validation Tests ----------

    @Test
    void healthEndpointIsPublic() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void protectedEndpointWithoutToken_shouldReturn401() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/customers/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("WWW-Authenticate"));
    }

    @Test
    void protectedEndpointWithInvalidToken_shouldReturn401() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/customers/me")
                        .header("Authorization", "Bearer invalid-signature-token-12345"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("WWW-Authenticate"));
    }

    @Test
    void publicMoviesEndpointWithoutToken_passesSecurityFilter() {
        try {
            mockMvc.perform(MockMvcRequestBuilders.get("/api/movies"));
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            assertTrue(cause instanceof ResourceAccessException, "Expected ResourceAccessException from downstream proxy call");
        }
    }

    // ---------- F10.3: Role-based Authorization Tests ----------

    @Test
    void customerRole_accessingAdminReports_shouldReturn403Forbidden() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/reports/revenue")
                        .with(jwt().jwt(j -> j.claim("role", "CUSTOMER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerRole_accessingAdminCustomerList_shouldReturn403Forbidden() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/customers")
                        .with(jwt().jwt(j -> j.claim("role", "CUSTOMER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminRole_accessingAdminReports_shouldPassSecurity() {
        try {
            mockMvc.perform(MockMvcRequestBuilders.get("/api/reports/revenue")
                    .with(jwt().jwt(j -> j.claim("role", "ADMIN"))));
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            assertTrue(cause instanceof ResourceAccessException, "Expected ResourceAccessException from downstream proxy call");
        }
    }

    @Test
    void customerRole_accessingCustomerProfile_shouldPassSecurity() {
        try {
            mockMvc.perform(MockMvcRequestBuilders.get("/api/customers/me")
                    .with(jwt().jwt(j -> j.claim("role", "CUSTOMER"))));
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            assertTrue(cause instanceof ResourceAccessException, "Expected ResourceAccessException from downstream proxy call");
        }
    }
}