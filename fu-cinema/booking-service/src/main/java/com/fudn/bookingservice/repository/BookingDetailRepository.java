package com.fudn.bookingservice.repository;

import com.fudn.bookingservice.model.BookingDetail;
import com.fudn.bookingservice.model.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingDetailRepository extends JpaRepository<BookingDetail, Long> {

    @Query("""
            select d.seatCode from BookingDetail d
            where d.showtimeId = :showtimeId
              and d.booking.bookingStatus = :status
            """)
    List<String> findSeatCodesByShowtime(@Param("showtimeId") String showtimeId,
                                         @Param("status") BookingStatus status);

    // F9.1: Doanh thu theo tung phim sap xep giam dan theo tong tien
    @Query("""
            select d.movieId as movieId,
                   d.movieTitle as movieTitle,
                   count(d.bookingDetailId) as ticketCount,
                   coalesce(sum(d.price), 0) as totalRevenue
            from BookingDetail d
            where d.booking.bookingStatus = :status
              and d.booking.bookingDate >= :from
              and d.booking.bookingDate < :to
            group by d.movieId, d.movieTitle
            order by sum(d.price) desc
            """)
    List<MovieRevenueProjection> findRevenueByMovie(@Param("status") BookingStatus status,
                                                    @Param("from") LocalDateTime from,
                                                    @Param("to") LocalDateTime to);

    // F9.1: Dem tong so ve da ban cho cac booking hop le trong khoang thoi gian
    @Query("""
            select count(d.bookingDetailId) from BookingDetail d
            where d.booking.bookingStatus = :status
              and d.booking.bookingDate >= :from
              and d.booking.bookingDate < :to
            """)
    long countTicketsByStatusAndDateRange(@Param("status") BookingStatus status,
                                          @Param("from") LocalDateTime from,
                                          @Param("to") LocalDateTime to);
}