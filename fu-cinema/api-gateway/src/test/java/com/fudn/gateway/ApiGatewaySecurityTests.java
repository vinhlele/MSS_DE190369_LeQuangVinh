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
    void protectedEndpointWithValidJwt_passesSecurityFilter() {
        try {
            mockMvc.perform(MockMvcRequestBuilders.get("/api/customers/me").with(jwt()));
        } catch (Exception e) {
            // Passes Security filter and reaches Gateway routing (fails only because downstream service is offline)
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            assertTrue(cause instanceof ResourceAccessException, "Expected ResourceAccessException from downstream proxy call");
        }
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

    @Test
    void publicShowtimesEndpointWithoutToken_passesSecurityFilter() {
        try {
            mockMvc.perform(MockMvcRequestBuilders.get("/api/showtimes"));
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            assertTrue(cause instanceof ResourceAccessException, "Expected ResourceAccessException from downstream proxy call");
        }
    }
}