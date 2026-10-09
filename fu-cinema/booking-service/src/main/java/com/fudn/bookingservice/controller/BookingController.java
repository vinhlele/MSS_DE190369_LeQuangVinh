package com.fudn.bookingservice.controller;

import com.fudn.bookingservice.dto.BookingResponse;
import com.fudn.bookingservice.dto.CreateBookingRequest;
import com.fudn.bookingservice.dto.SeatMapResponse;
import com.fudn.bookingservice.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private static final String USER_ID = "X-User-Id";
    private static final String USER_ROLE = "X-User-Role";

    private final BookingService bookingService;

    /**
     * Lay so do ghe va danh sach ghe da dat cho mot suat chieu.
     */
    @GetMapping("/showtimes/{showtimeId}/seats")
    public SeatMapResponse getSeatMap(@PathVariable String showtimeId) {
        return bookingService.getSeatMap(showtimeId);
    }

    /**
     * Dat ve: nhan danh sach ve (showtimeId + seatCode), gia tien tinh tren server tu thong tin suat chieu.
     * Customer id lay tu header xac thuc X-User-Id.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse create(
            @RequestHeader(USER_ID) Long userId,
            @Valid @RequestBody CreateBookingRequest request) {
        return bookingService.create(userId, request);
    }

    /**
     * TODO 8.2: Lay lich su dat ve cua chinh customer dang dang nhap.
     */
    @GetMapping("/my")
    public List<BookingResponse> getMyBookings(@RequestHeader(USER_ID) Long userId) {
        return bookingService.getMyBookings(userId);
    }

    /**
     * TODO 8.2: Lay danh sach dat ve.
     * ADMIN co the xem tat ca hoac loc theo customerId; CUSTOMER chi xem cua minh.
     */
    @GetMapping
    public List<BookingResponse> getBookings(
            @RequestHeader(USER_ID) Long userId,
            @RequestHeader(value = USER_ROLE, required = false) String role,
            @RequestParam(required = false) Long customerId) {
        return bookingService.getBookings(userId, role, customerId);
    }

    /**
     * TODO 8.2: Xem chi tiet dat ve theo ID. Chinh chu (hoac ADMIN) moi co quyen xem.
     */
    @GetMapping("/{id}")
    public BookingResponse getById(
            @PathVariable Long id,
            @RequestHeader(USER_ID) Long userId,
            @RequestHeader(value = USER_ROLE, required = false) String role) {
        return bookingService.getById(id, userId, role);
    }

    /**
     * TODO 8.4: Huy dat ve theo ID (DELETE /api/bookings/{id}).
     */
    @DeleteMapping("/{id}")
    public BookingResponse cancel(
            @PathVariable Long id,
            @RequestHeader(USER_ID) Long userId,
            @RequestHeader(value = USER_ROLE, required = false) String role) {
        return bookingService.cancel(id, userId, role);
    }

    /**
     * TODO 8.4: Huy dat ve theo ID (PUT /api/bookings/{id}/cancel).
     */
    @PutMapping("/{id}/cancel")
    public BookingResponse cancelPut(
            @PathVariable Long id,
            @RequestHeader(USER_ID) Long userId,
            @RequestHeader(value = USER_ROLE, required = false) String role) {
        return bookingService.cancel(id, userId, role);
    }
}