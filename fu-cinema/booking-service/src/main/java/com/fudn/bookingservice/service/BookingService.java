package com.fudn.bookingservice.service;

import com.fudn.bookingservice.client.MovieClient;
import com.fudn.bookingservice.dto.*;
import com.fudn.bookingservice.exception.ApiException;
import com.fudn.bookingservice.model.Booking;
import com.fudn.bookingservice.model.BookingDetail;
import com.fudn.bookingservice.model.BookingStatus;
import com.fudn.bookingservice.repository.BookingDetailRepository;
import com.fudn.bookingservice.repository.BookingRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookingService {

    private static final String ROLE_ADMIN = "ADMIN";

    private final BookingRepository bookingRepository;
    private final BookingDetailRepository bookingDetailRepository;
    private final MovieClient movieClient;

    @Transactional
    public BookingResponse create(Long customerId, CreateBookingRequest request) {
        Booking booking = new Booking();
        booking.setCustomerId(customerId);
        booking.setBookingDate(LocalDateTime.now());
        booking.setBookingStatus(BookingStatus.CONFIRMED);
        BigDecimal total = BigDecimal.ZERO;

        for (BookingItemRequest item : request.items()) {
            ShowtimeResponse st = fetchShowtime(item.showtimeId());

            BookingDetail detail = new BookingDetail();
            detail.setShowtimeId(st.showtimeId());
            detail.setSeatCode(item.seatCode());
            detail.setPrice(st.ticketPrice());
            detail.setMovieId(st.movieId());
            detail.setMovieTitle(st.movieTitle());
            detail.setRoomName(st.roomName());
            detail.setShowtimeStart(st.startTime());
            booking.addDetail(detail);

            total = total.add(st.ticketPrice());
        }

        booking.setTotalPrice(total);
        Booking saved = bookingRepository.save(booking);
        log.info("Booking {} created for customer {} with {} ticket(s), total {}",
                saved.getBookingId(), customerId, saved.getDetails().size(), total);
        return BookingResponse.from(saved);
    }

    public BookingResponse getById(Long bookingId, Long userId, String role) {
        return BookingResponse.from(findAccessible(bookingId, userId, role));
    }

    public ShowtimeResponse fetchShowtime(String showtimeId) {
        try {
            return movieClient.getShowtime(showtimeId);
        } catch (FeignException.NotFound e) {
            throw ApiException.notFound("Showtime not found with id: " + showtimeId);
        } catch (FeignException e) {
            log.error("Cannot call movie-service: {}", e.getMessage());
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Movie service is unavailable. Please try again later.");
        }
    }

    private Booking findAccessible(Long bookingId, Long userId, String role) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> ApiException.notFound("Booking not found with id: " + bookingId));
        if (!ROLE_ADMIN.equals(role) && !booking.getCustomerId().equals(userId)) {
            throw ApiException.forbidden("You can only access your own bookings");
        }
        return booking;
    }
}