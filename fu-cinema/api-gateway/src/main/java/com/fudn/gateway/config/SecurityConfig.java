package com.fudn.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Cau hinh Security cho API Gateway.
 * - Stateless, tat CSRF.
 * - F10.2: Xac thuc JWT tren cac route can bao ve.
 * - F10.3: Phan quyen RBAC theo claim "role" trong JWT (ROLE_ADMIN, ROLE_CUSTOMER).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String ROLE_ADMIN = "ADMIN";

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        // 1. Public endpoints
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/customers/register").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/movies/**", "/api/genres/**", "/api/rooms/**", "/api/showtimes/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/bookings/showtimes/*/seats").permitAll()

                        // 2. Customer profile (authenticated, khong gioi han role)
                        .requestMatchers("/api/customers/me", "/api/customers/me/**").authenticated()

                        // 3. Admin Customer management (F10.3)
                        .requestMatchers("/api/customers", "/api/customers/**").hasRole(ROLE_ADMIN)

                        // 4. Admin Movie / Genre / Room / Showtime management (F10.3: POST, PUT, DELETE)
                        .requestMatchers(HttpMethod.POST, "/api/movies/**", "/api/genres/**", "/api/rooms/**", "/api/showtimes/**").hasRole(ROLE_ADMIN)
                        .requestMatchers(HttpMethod.PUT, "/api/movies/**", "/api/genres/**", "/api/rooms/**", "/api/showtimes/**").hasRole(ROLE_ADMIN)
                        .requestMatchers(HttpMethod.DELETE, "/api/movies/**", "/api/genres/**", "/api/rooms/**", "/api/showtimes/**").hasRole(ROLE_ADMIN)

                        // 5. Admin Reports (F10.3)
                        .requestMatchers("/api/reports/**", "/api/bookings/reports/**").hasRole(ROLE_ADMIN)

                        // 6. Booking endpoints (authenticated)
                        .requestMatchers("/api/bookings/**").authenticated()

                        // 7. Cac endpoint con lai bat buoc dang nhap
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt ->
                        jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName("role");
        authoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return converter;
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        SecretKey key = new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }
}