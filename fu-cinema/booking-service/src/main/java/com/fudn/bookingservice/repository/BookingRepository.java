package com.fudn.bookingservice.repository;

import com.fudn.bookingservice.model.Booking;
import com.fudn.bookingservice.model.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    // TODO 8.1: Truy van lich su dat ve cua mot khach hang sap xep theo thoi gian dat ve giam dan
    List<Booking> findByCustomerIdOrderByBookingDateDesc(Long customerId);

    // TODO 8.1: Truy van don dat ve theo ID va customerId de dam bao quyen so huu (ownership)
    Optional<Booking> findByBookingIdAndCustomerId(Long bookingId, Long customerId);

    List<Booking> findAllByOrderByBookingDateDesc();

    @Query("""
            select b from Booking b
            where b.bookingStatus = :status
              and b.bookingDate >= :from
              and b.bookingDate < :to
            order by b.bookingDate desc
            """)
    List<Booking> findForReport(@Param("status") BookingStatus status,
                                @Param("from") LocalDateTime from,
                                @Param("to") LocalDateTime to);
}