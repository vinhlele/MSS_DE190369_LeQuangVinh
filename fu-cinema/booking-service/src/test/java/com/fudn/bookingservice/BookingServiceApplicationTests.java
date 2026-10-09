package com.fudn.bookingservice;

import com.fudn.bookingservice.controller.BookingController;
import com.fudn.bookingservice.dto.*;
import com.fudn.bookingservice.exception.ApiException;
import com.fudn.bookingservice.exception.GlobalExceptionHandler;
import com.fudn.bookingservice.model.BookingStatus;
import com.fudn.bookingservice.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BookingServiceApplicationTests {

    @Mock
    private BookingService bookingService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        BookingController controller = new BookingController(bookingService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getSeatMap_success() throws Exception {
        SeatMapResponse response = new SeatMapResponse("st-001", "Inception", "Room 1",
                LocalDateTime.of(2026, 12, 1, 10, 0), 8, 10, 80, 78, List.of("A1", "A2"));
        when(bookingService.getSeatMap("st-001")).thenReturn(response);

        mockMvc.perform(get("/api/bookings/showtimes/st-001/seats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.showtimeId").value("st-001"))
                .andExpect(jsonPath("$.totalSeats").value(80))
                .andExpect(jsonPath("$.availableSeats").value(78))
                .andExpect(jsonPath("$.bookedSeats.length()").value(2));
    }

    @Test
    void createBooking_success() throws Exception {
        BookingDetailResponse detail = new BookingDetailResponse("st-001", "mov-01", "Inception", "Room 1",
                LocalDateTime.of(2026, 12, 1, 10, 0), "C5", new BigDecimal("100000"));
        BookingResponse response = new BookingResponse(100L, LocalDateTime.now(), 1L,
                new BigDecimal("100000"), BookingStatus.CONFIRMED, List.of(detail));

        when(bookingService.create(eq(1L), any(CreateBookingRequest.class))).thenReturn(response);

        String json = """
                {
                    "items": [
                        { "showtimeId": "st-001", "seatCode": "C5" }
                    ]
                }
                """;

        mockMvc.perform(post("/api/bookings")
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bookingId").value(100))
                .andExpect(jsonPath("$.customerId").value(1))
                .andExpect(jsonPath("$.totalPrice").value(100000))
                .andExpect(jsonPath("$.bookingStatus").value("CONFIRMED"));
    }

    @Test
    void createBooking_invalidSeatFormat_returnsBadRequest() throws Exception {
        String json = """
                {
                    "items": [
                        { "showtimeId": "st-001", "seatCode": "INVALID_SEAT" }
                    ]
                }
                """;

        mockMvc.perform(post("/api/bookings")
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void createBooking_emptyItems_returnsBadRequest() throws Exception {
        String json = """
                {
                    "items": []
                }
                """;

        mockMvc.perform(post("/api/bookings")
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void createBooking_blankShowtimeId_returnsBadRequest() throws Exception {
        String json = """
                {
                    "items": [
                        { "showtimeId": "", "seatCode": "A1" }
                    ]
                }
                """;

        mockMvc.perform(post("/api/bookings")
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    // ---------- TODO 8.2 Tests ----------

    @Test
    void getMyBookings_success() throws Exception {
        BookingDetailResponse detail = new BookingDetailResponse("st-001", "mov-01", "Inception", "Room 1",
                LocalDateTime.of(2026, 12, 1, 10, 0), "C5", new BigDecimal("100000"));
        BookingResponse response = new BookingResponse(100L, LocalDateTime.now(), 1L,
                new BigDecimal("100000"), BookingStatus.CONFIRMED, List.of(detail));

        when(bookingService.getMyBookings(1L)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/bookings/my")
                        .header("X-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].bookingId").value(100))
                .andExpect(jsonPath("$[0].customerId").value(1));
    }

    @Test
    void getBookings_admin_success() throws Exception {
        BookingDetailResponse detail = new BookingDetailResponse("st-001", "mov-01", "Inception", "Room 1",
                LocalDateTime.of(2026, 12, 1, 10, 0), "C5", new BigDecimal("100000"));
        BookingResponse response = new BookingResponse(100L, LocalDateTime.now(), 1L,
                new BigDecimal("100000"), BookingStatus.CONFIRMED, List.of(detail));

        when(bookingService.getBookings(eq(1L), eq("ADMIN"), isNull())).thenReturn(List.of(response));

        mockMvc.perform(get("/api/bookings")
                        .header("X-User-Id", 1L)
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].bookingId").value(100));
    }

    @Test
    void getById_success() throws Exception {
        BookingDetailResponse detail = new BookingDetailResponse("st-001", "mov-01", "Inception", "Room 1",
                LocalDateTime.of(2026, 12, 1, 10, 0), "C5", new BigDecimal("100000"));
        BookingResponse response = new BookingResponse(100L, LocalDateTime.now(), 1L,
                new BigDecimal("100000"), BookingStatus.CONFIRMED, List.of(detail));

        when(bookingService.getById(eq(100L), eq(1L), isNull())).thenReturn(response);

        mockMvc.perform(get("/api/bookings/100")
                        .header("X-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingId").value(100))
                .andExpect(jsonPath("$.customerId").value(1));
    }

    @Test
    void getById_notFound_returnsNotFound() throws Exception {
        when(bookingService.getById(eq(999L), eq(1L), isNull()))
                .thenThrow(ApiException.notFound("Booking not found with id: 999"));

        mockMvc.perform(get("/api/bookings/999")
                        .header("X-User-Id", 1L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void getById_forbidden_otherCustomer_returnsForbidden() throws Exception {
        when(bookingService.getById(eq(100L), eq(2L), isNull()))
                .thenThrow(ApiException.forbidden("You can only access your own bookings"));

        mockMvc.perform(get("/api/bookings/100")
                        .header("X-User-Id", 2L))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }
}