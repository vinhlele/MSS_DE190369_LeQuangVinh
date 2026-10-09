package com.fudn.bookingservice.dto;

import java.math.BigDecimal;

public record MovieRevenueResponse(
        String movieId,
        String movieTitle,
        long ticketCount,
        BigDecimal totalRevenue) {
}