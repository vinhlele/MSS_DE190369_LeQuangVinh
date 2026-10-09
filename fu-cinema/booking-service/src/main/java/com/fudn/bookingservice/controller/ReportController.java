package com.fudn.bookingservice.controller;

import com.fudn.bookingservice.dto.RevenueReportResponse;
import com.fudn.bookingservice.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping({"/api/reports", "/api/bookings/reports"})
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping({"", "/revenue"})
    public RevenueReportResponse getRevenueReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate start = startDate != null ? startDate : from;
        LocalDate end = endDate != null ? endDate : to;
        return reportService.getRevenueReport(start, end);
    }
}