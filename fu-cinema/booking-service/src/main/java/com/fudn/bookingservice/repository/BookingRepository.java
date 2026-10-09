package com.fudn.bookingservice.repository;

import com.fudn.bookingservice.model.Booking;
import com.fudn.bookingservice.model.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    // TODO 8.1: Truy van lich su dat ve cua mot khach hang sap xep theo thoi gian dat ve giam dan
    List<Booking> findByCustomerIdOrderByBookingDateDesc(Long customerId);

    // TODO 8.1: Truy van don dat ve theo ID va customerId de dam bao quyen so huu (ownership)
    Optional<Booking> findByBookingIdAndCustomerId(Long bookingId, Long customerId);

    List<Booking> findAllByOrderByBookingDateDesc();

    // F9.1: Lay toan bo booking cho bao cao trong khoang thoi gian
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

    // F9.1: Dem so luong booking theo trang thai va khoang thoi gian
    @Query("""
            select count(b.bookingId) from Booking b
            where b.bookingStatus = :status
              and b.bookingDate >= :from
              and b.bookingDate < :to
            """)
    long countBookingsByStatusAndDateRange(@Param("status") BookingStatus status,
                                           @Param("from") LocalDateTime from,
                                           @Param("to") LocalDateTime to);

    // F9.1: Tong doanh thu tu cac booking theo trang thai va khoang thoi gian
    @Query("""
            select coalesce(sum(b.totalPrice), 0) from Booking b
            where b.bookingStatus = :status
              and b.bookingDate >= :from
              and b.bookingDate < :to
            """)
    BigDecimal sumRevenueByStatusAndDateRange(@Param("status") BookingStatus status,
                                              @Param("from") LocalDateTime from,
                                              @Param("to") LocalDateTime to);
}