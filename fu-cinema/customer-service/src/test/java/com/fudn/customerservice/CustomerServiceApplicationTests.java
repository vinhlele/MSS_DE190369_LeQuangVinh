package com.fudn.customerservice;

import com.fudn.customerservice.config.PasswordConfig;
import com.fudn.customerservice.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.junit.jupiter.api.Assertions.*;

class CustomerServiceApplicationTests {

    @Test
    void contextLoads() {
    }

    @Test
    void passwordEncoder_shouldEncodeAndMatch() {
        PasswordEncoder encoder = new PasswordConfig().passwordEncoder();
        String raw = "samplePassword123";
        String encoded = encoder.encode(raw);

        assertNotEquals(raw, encoded);
        assertTrue(encoder.matches(raw, encoded));
    }

    @Test
    void jwtService_shouldGenerateAndDecodeToken() {
        String secret = "fu-cinema-booking-system-secret-key-2026-mss301";
        JwtService jwtService = new JwtService(secret, 60);

        String token = jwtService.generateToken(1L, "an@gmail.com", "CUSTOMER");
        assertNotNull(token);
        assertFalse(token.isBlank());

        Jwt decoded = jwtService.decodeToken(token);
        assertEquals("an@gmail.com", decoded.getSubject());
        assertEquals(Long.valueOf(1L), decoded.<Long>getClaim("uid"));
        assertEquals("CUSTOMER", decoded.getClaim("role"));
        assertEquals("fu-cinema", decoded.getClaimAsString("iss"));
    }
}