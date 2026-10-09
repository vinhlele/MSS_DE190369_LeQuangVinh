package com.fudn.gateway.routes;

import com.fudn.gateway.filter.UserHeaderFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RequestPredicates;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.uri;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;

/**
 * Routing rules cho API Gateway (Spring Cloud Gateway Server Web MVC).
 * - customer-service (8081): /api/auth/**, /api/customers/**
 * - movie-service (8082): /api/movies/**, /api/genres/**, /api/rooms/**, /api/showtimes/**
 * - booking-service (8083): /api/bookings/**, /api/reports/**
 * - F10.4: filter UserHeaderFilter loai bo header gia mao va chuyen tiep identity hop le tu JWT.
 */
@Configuration(proxyBeanMethods = false)
public class Routes {

    @Value("${services.customer.url:http://localhost:8081}")
    private String customerServiceUrl;

    @Value("${services.movie.url:http://localhost:8082}")
    private String movieServiceUrl;

    @Value("${services.booking.url:http://localhost:8083}")
    private String bookingServiceUrl;

    private final UserHeaderFilter userHeaderFilter;

    public Routes(UserHeaderFilter userHeaderFilter) {
        this.userHeaderFilter = userHeaderFilter;
    }

    @Bean
    public RouterFunction<ServerResponse> customerAuthRoute() {
        return route("customer_auth_route")
                .route(RequestPredicates.path("/api/auth/**"), http())
                .before(uri(customerServiceUrl))
                .filter(userHeaderFilter)
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> customerServiceRoute() {
        return route("customer_service_route")
                .route(RequestPredicates.path("/api/customers/**"), http())
                .before(uri(customerServiceUrl))
                .filter(userHeaderFilter)
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> movieServiceRoute() {
        return route("movie_service_route")
                .route(RequestPredicates.path("/api/movies/**")
                        .or(RequestPredicates.path("/api/genres/**"))
                        .or(RequestPredicates.path("/api/rooms/**"))
                        .or(RequestPredicates.path("/api/showtimes/**")), http())
                .before(uri(movieServiceUrl))
                .filter(userHeaderFilter)
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> bookingServiceRoute() {
        return route("booking_service_route")
                .route(RequestPredicates.path("/api/bookings/**")
                        .or(RequestPredicates.path("/api/reports/**")), http())
                .before(uri(bookingServiceUrl))
                .filter(userHeaderFilter)
                .build();
    }
}