package com.fudn.bookingservice;

import com.fudn.bookingservice.client.MovieClient;
import com.fudn.bookingservice.dto.BookingResponse;
import com.fudn.bookingservice.exception.ApiException;
import com.fudn.bookingservice.model.Booking;
import com.fudn.bookingservice.model.BookingDetail;
import com.fudn.bookingservice.model.BookingStatus;
import com.fudn.bookingservice.repository.BookingDetailRepository;
import com.fudn.bookingservice.repository.BookingRepository;
import com.fudn.bookingservice.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceCancellationTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BookingDetailRepository bookingDetailRepository;

    @Mock
    private MovieClient movieClient;

    @InjectMocks
    private BookingService bookingService;

    private Booking sampleBooking;
    private BookingDetail sampleDetail;

    @BeforeEach
    void setUp() {
        sampleBooking = new Booking();
        sampleBooking.setBookingId(1L);
        sampleBooking.setCustomerId(10L);
        sampleBooking.setBookingDate(LocalDateTime.now().minusDays(1));
        sampleBooking.setBookingStatus(BookingStatus.CONFIRMED);
        sampleBooking.setTotalPrice(BigDecimal.valueOf(100000));

        sampleDetail = new BookingDetail();
        sampleDetail.setBookingDetailId(1L);
        sampleDetail.setShowtimeId("st-1");
        sampleDetail.setSeatCode("A1");
        sampleDetail.setPrice(BigDecimal.valueOf(100000));
        sampleDetail.setMovieId("m-1");
        sampleDetail.setMovieTitle("Avatar");
        sampleDetail.setRoomName("Room 1");
        sampleDetail.setShowtimeStart(LocalDateTime.now().plusHours(5)); // 5 hours in future
        sampleBooking.addDetail(sampleDetail);
    }

    @Test
    void cancel_customerAllowed_success() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(sampleBooking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookingResponse response = bookingService.cancel(1L, 10L, "CUSTOMER");

        assertNotNull(response);
        assertEquals(BookingStatus.CANCELLED, response.bookingStatus());
    }

    @Test
    void cancel_tooLate_throwsBadRequest() {
        // Showtime starts in 1 hour (less than 2 hours)
        sampleDetail.setShowtimeStart(LocalDateTime.now().plusHours(1));
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(sampleBooking));

        ApiException ex = assertThrows(ApiException.class, () ->
                bookingService.cancel(1L, 10L, "CUSTOMER"));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("at least 2 hours"));
    }

    @Test
    void cancel_alreadyCancelled_throwsBadRequest() {
        sampleBooking.setBookingStatus(BookingStatus.CANCELLED);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(sampleBooking));

        ApiException ex = assertThrows(ApiException.class, () ->
                bookingService.cancel(1L, 10L, "CUSTOMER"));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("already cancelled"));
    }

    @Test
    void cancel_otherCustomer_throwsForbidden() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(sampleBooking));

        ApiException ex = assertThrows(ApiException.class, () ->
                bookingService.cancel(1L, 999L, "CUSTOMER")); // caller is 999L, owner is 10L

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    void cancel_adminAllowedEvenIfLessThanTwoHours_success() {
        // Showtime starts in 30 minutes
        sampleDetail.setShowtimeStart(LocalDateTime.now().plusMinutes(30));
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(sampleBooking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookingResponse response = bookingService.cancel(1L, 999L, "ADMIN");

        assertNotNull(response);
        assertEquals(BookingStatus.CANCELLED, response.bookingStatus());
    }
}