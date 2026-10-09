package com.fudn.bookingservice.service;

import com.fudn.bookingservice.dto.MovieRevenueResponse;
import com.fudn.bookingservice.dto.RevenueReportResponse;
import com.fudn.bookingservice.exception.ApiException;
import com.fudn.bookingservice.model.BookingStatus;
import com.fudn.bookingservice.repository.BookingDetailRepository;
import com.fudn.bookingservice.repository.BookingRepository;
import com.fudn.bookingservice.repository.MovieRevenueProjection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final BookingRepository bookingRepository;
    private final BookingDetailRepository bookingDetailRepository;

    public RevenueReportResponse getRevenueReport(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) {
            startDate = LocalDate.now().minusDays(30);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        if (startDate.isAfter(endDate)) {
            throw ApiException.badRequest("startDate cannot be after endDate");
        }

        LocalDateTime from = startDate.atStartOfDay();
        LocalDateTime to = endDate.plusDays(1).atStartOfDay();

        long totalBookings = bookingRepository.countBookingsByStatusAndDateRange(BookingStatus.CONFIRMED, from, to);
        long totalTickets = bookingDetailRepository.countTicketsByStatusAndDateRange(BookingStatus.CONFIRMED, from, to);
        BigDecimal totalRevenue = bookingRepository.sumRevenueByStatusAndDateRange(BookingStatus.CONFIRMED, from, to);

        List<MovieRevenueProjection> movieProjections = bookingDetailRepository.findRevenueByMovie(BookingStatus.CONFIRMED, from, to);
        List<MovieRevenueResponse> byMovie = movieProjections.stream()
                .map(p -> new MovieRevenueResponse(
                        p.getMovieId(),
                        p.getMovieTitle(),
                        p.getTicketCount() != null ? p.getTicketCount() : 0L,
                        p.getTotalRevenue() != null ? p.getTotalRevenue() : BigDecimal.ZERO
                ))
                .toList();

        log.info("Generated revenue report from {} to {}: {} bookings, {} tickets, total {}",
                startDate, endDate, totalBookings, totalTickets, totalRevenue);

        return new RevenueReportResponse(startDate, endDate, totalBookings, totalTickets, totalRevenue, byMovie);
    }
}