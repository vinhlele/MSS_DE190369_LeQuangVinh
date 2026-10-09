package com.fudn.bookingservice;

import com.fudn.bookingservice.controller.ReportController;
import com.fudn.bookingservice.dto.MovieRevenueResponse;
import com.fudn.bookingservice.dto.RevenueReportResponse;
import com.fudn.bookingservice.exception.ApiException;
import com.fudn.bookingservice.exception.GlobalExceptionHandler;
import com.fudn.bookingservice.service.ReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ReportControllerTest {

    @Mock
    private ReportService reportService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ReportController controller = new ReportController(reportService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getRevenueReport_success() throws Exception {
        LocalDate start = LocalDate.of(2026, 10, 1);
        LocalDate end = LocalDate.of(2026, 10, 10);

        MovieRevenueResponse movie1 = new MovieRevenueResponse("m-1", "Avatar", 50L, BigDecimal.valueOf(5000000));
        MovieRevenueResponse movie2 = new MovieRevenueResponse("m-2", "Inception", 30L, BigDecimal.valueOf(3000000));

        RevenueReportResponse report = new RevenueReportResponse(start, end, 40L, 80L,
                BigDecimal.valueOf(8000000), List.of(movie1, movie2));

        when(reportService.getRevenueReport(eq(start), eq(end))).thenReturn(report);

        mockMvc.perform(get("/api/reports/revenue")
                        .param("startDate", "2026-10-01")
                        .param("endDate", "2026-10-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalBookings").value(40))
                .andExpect(jsonPath("$.totalTickets").value(80))
                .andExpect(jsonPath("$.totalRevenue").value(8000000))
                .andExpect(jsonPath("$.byMovie.length()").value(2))
                .andExpect(jsonPath("$.byMovie[0].movieTitle").value("Avatar"))
                .andExpect(jsonPath("$.byMovie[0].totalRevenue").value(5000000));
    }

    @Test
    void getRevenueReport_viaBookingsReportsRoute_success() throws Exception {
        LocalDate start = LocalDate.of(2026, 10, 1);
        LocalDate end = LocalDate.of(2026, 10, 10);

        RevenueReportResponse report = new RevenueReportResponse(start, end, 10L, 20L,
                BigDecimal.valueOf(2000000), List.of());

        when(reportService.getRevenueReport(eq(start), eq(end))).thenReturn(report);

        mockMvc.perform(get("/api/bookings/reports/revenue")
                        .param("from", "2026-10-01")
                        .param("to", "2026-10-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalBookings").value(10))
                .andExpect(jsonPath("$.totalTickets").value(20));
    }

    @Test
    void getRevenueReport_invalidDateRange_returnsBadRequest() throws Exception {
        when(reportService.getRevenueReport(any(), any()))
                .thenThrow(ApiException.badRequest("startDate cannot be after endDate"));

        mockMvc.perform(get("/api/reports/revenue")
                        .param("startDate", "2026-10-10")
                        .param("endDate", "2026-10-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("startDate cannot be after endDate"));
    }
}