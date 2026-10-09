package com.fudn.bookingservice.repository;

import java.math.BigDecimal;

public interface MovieRevenueProjection {
    String getMovieId();
    String getMovieTitle();
    Long getTicketCount();
    BigDecimal getTotalRevenue();
}