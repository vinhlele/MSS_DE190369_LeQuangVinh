package com.fudn.gateway;

import com.fudn.gateway.filter.UserHeaderFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.time.Instant;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class UserHeaderFilterTest {

    private final UserHeaderFilter filter = new UserHeaderFilter();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void filter_unauthenticatedRequestWithSpoofedHeaders_stripsHeaders() throws Exception {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/api/movies");
        servletRequest.addHeader("X-User-Id", "999");
        servletRequest.addHeader("X-User-Email", "fake@admin.com");
        servletRequest.addHeader("X-User-Role", "ADMIN");

        ServerRequest request = ServerRequest.create(servletRequest, Collections.emptyList());
        AtomicReference<ServerRequest> capturedRequest = new AtomicReference<>();

        filter.filter(request, req -> {
            capturedRequest.set(req);
            return ServerResponse.ok().build();
        });

        ServerRequest result = capturedRequest.get();
        assertNotNull(result);
        assertTrue(result.headers().header("X-User-Id").isEmpty(), "X-User-Id should be stripped");
        assertTrue(result.headers().header("X-User-Email").isEmpty(), "X-User-Email should be stripped");
        assertTrue(result.headers().header("X-User-Role").isEmpty(), "X-User-Role should be stripped");
    }

    @Test
    void filter_authenticatedRequestWithSpoofedHeaders_overwritesWithJwtClaims() throws Exception {
        Jwt jwt = Jwt.withTokenValue("mock-token")
                .header("alg", "HS256")
                .claim("uid", 123L)
                .claim("sub", "customer@cinema.com")
                .claim("role", "CUSTOMER")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt);
        SecurityContextHolder.getContext().setAuthentication(auth);

        MockHttpServletRequest servletRequest = new MockHttpServletRequest("POST", "/api/bookings");
        servletRequest.addHeader("X-User-Id", "999");
        servletRequest.addHeader("X-User-Email", "hacker@evil.com");
        servletRequest.addHeader("X-User-Role", "ADMIN");

        ServerRequest request = ServerRequest.create(servletRequest, Collections.emptyList());
        AtomicReference<ServerRequest> capturedRequest = new AtomicReference<>();

        filter.filter(request, req -> {
            capturedRequest.set(req);
            return ServerResponse.ok().build();
        });

        ServerRequest result = capturedRequest.get();
        assertNotNull(result);
        assertEquals("123", result.headers().firstHeader("X-User-Id"));
        assertEquals("customer@cinema.com", result.headers().firstHeader("X-User-Email"));
        assertEquals("CUSTOMER", result.headers().firstHeader("X-User-Role"));
    }

    @Test
    void filter_authenticatedRequestWithoutHeaders_addsTrustedHeaders() throws Exception {
        Jwt jwt = Jwt.withTokenValue("mock-token")
                .header("alg", "HS256")
                .claim("uid", 456L)
                .claim("sub", "admin@cinema.com")
                .claim("role", "ADMIN")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt);
        SecurityContextHolder.getContext().setAuthentication(auth);

        MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/api/customers/me");
        ServerRequest request = ServerRequest.create(servletRequest, Collections.emptyList());
        AtomicReference<ServerRequest> capturedRequest = new AtomicReference<>();

        filter.filter(request, req -> {
            capturedRequest.set(req);
            return ServerResponse.ok().build();
        });

        ServerRequest result = capturedRequest.get();
        assertNotNull(result);
        assertEquals("456", result.headers().firstHeader("X-User-Id"));
        assertEquals("admin@cinema.com", result.headers().firstHeader("X-User-Email"));
        assertEquals("ADMIN", result.headers().firstHeader("X-User-Role"));
    }
}