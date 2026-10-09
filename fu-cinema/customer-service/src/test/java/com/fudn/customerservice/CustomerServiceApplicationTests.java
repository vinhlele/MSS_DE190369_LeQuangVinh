package com.fudn.customerservice;

import com.fudn.customerservice.config.PasswordConfig;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
}