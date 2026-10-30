package fu.de190369.reservationservice.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import fu.de190369.reservationservice.client.RoomClient;
import fu.de190369.reservationservice.common.ReservationConstants;
import fu.de190369.reservationservice.dto.*;
import fu.de190369.reservationservice.entity.Reservation;
import fu.de190369.reservationservice.entity.ReservationStatus;
import fu.de190369.reservationservice.repository.ReservationRepository;
import fu.de190369.reservationservice.service.ReservationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class ReservationServiceImpl implements ReservationService {

    private final ReservationRepository reservationRepository;
    private final RoomClient roomClient;
    private final ObjectMapper objectMapper;

    public ReservationServiceImpl(ReservationRepository reservationRepository,
                                  RoomClient roomClient,
                                  ObjectMapper objectMapper) {
        this.reservationRepository = reservationRepository;
        this.roomClient = roomClient;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public ApiResponseDTO createReservation(ReservationDTO dto) {
        // 1. Date validation: checkOutDate must be after checkInDate (status 2 -> HTTP 406)
        if (!dto.getCheckOutDate().isAfter(dto.getCheckInDate())) {
            return ApiResponseDTO.error(2, "checkOutDate must be after checkInDate");
        }

        // 2. Call RoomService to verify room exists (status 4 -> HTTP 404)
        RoomDTO room = fetchRoom(dto.getRoomId());
        if (room == null) {
            return ApiResponseDTO.error(4, "Room is not found");
        }

        // 3. Verify room has status AVAILABLE (status 5 -> HTTP 400)
        if (!ReservationConstants.ROOM_AVAILABLE.equalsIgnoreCase(room.getStatus())) {
            return ApiResponseDTO.error(5, "Room is not AVAILABLE for reservation");
        }

        // 4. Verify numberOfGuests does not exceed room capacity (status 2 -> HTTP 406)
        if (dto.getNumberOfGuests() > room.getCapacity()) {
            return ApiResponseDTO.error(2, "numberOfGuests exceeds room capacity of " + room.getCapacity());
        }

        // 5. Calculate totalAmount = pricePerNight x number of nights
        BigDecimal totalAmount = computeTotalAmount(room.getPricePerNight(), dto.getCheckInDate(), dto.getCheckOutDate());

        Reservation reservation = new Reservation();
        reservation.setGuestName(dto.getGuestName().trim());
        reservation.setGuestEmail(dto.getGuestEmail().trim());
        reservation.setGuestPhone(dto.getGuestPhone().trim());
        reservation.setCheckInDate(dto.getCheckInDate());
        reservation.setCheckOutDate(dto.getCheckOutDate());
        reservation.setNumberOfGuests(dto.getNumberOfGuests());
        reservation.setTotalAmount(totalAmount);
        reservation.setRoomId(dto.getRoomId());

        // Default status: CONFIRMED
        if (dto.getStatus() == null || dto.getStatus().isBlank()) {
            reservation.setStatus(ReservationStatus.CONFIRMED);
        } else {
            reservation.setStatus(ReservationStatus.valueOf(dto.getStatus().trim().toUpperCase()));
        }

        Reservation saved = reservationRepository.save(reservation);
        return ApiResponseDTO.of(1, "Reservation created successfully", toDTO(saved));
    }

    @Override
    @Transactional
    public ApiResponseDTO updateReservation(Long reservationId, ReservationDTO dto) {
        Reservation reservation = reservationRepository.findById(reservationId).orElse(null);
        if (reservation == null) {
            return ApiResponseDTO.error(4, "Reservation is not found");
        }

        // Determine effective dates and fields for validation before mutating
        LocalDate effectiveCheckIn = (dto.getCheckInDate() != null) ? dto.getCheckInDate() : reservation.getCheckInDate();
        LocalDate effectiveCheckOut = (dto.getCheckOutDate() != null) ? dto.getCheckOutDate() : reservation.getCheckOutDate();
        Long effectiveRoomId = (dto.getRoomId() != null) ? dto.getRoomId() : reservation.getRoomId();
        Integer effectiveGuests = (dto.getNumberOfGuests() != null) ? dto.getNumberOfGuests() : reservation.getNumberOfGuests();

        if (!effectiveCheckOut.isAfter(effectiveCheckIn)) {
            return ApiResponseDTO.error(2, "checkOutDate must be after checkInDate");
        }

        if (dto.getNumberOfGuests() != null && (dto.getNumberOfGuests() < 1 || dto.getNumberOfGuests() > 10)) {
            return ApiResponseDTO.error(2, "numberOfGuests must be between 1 and 10");
        }

        if (dto.getStatus() != null) {
            ReservationStatus parsedStatus = parseStatus(dto.getStatus());
            if (parsedStatus == null) {
                return ApiResponseDTO.error(2, "status must be CONFIRMED, CHECKED_IN, CHECKED_OUT, or CANCELLED");
            }
            reservation.setStatus(parsedStatus);
        }

        // Fetch room to re-validate existence, capacity and recalculate amount
        RoomDTO room = fetchRoom(effectiveRoomId);
        if (room == null) {
            return ApiResponseDTO.error(4, "Room is not found");
        }

        // If roomId changed, verify the new room is AVAILABLE
        if (dto.getRoomId() != null && !dto.getRoomId().equals(reservation.getRoomId())) {
            if (!ReservationConstants.ROOM_AVAILABLE.equalsIgnoreCase(room.getStatus())) {
                return ApiResponseDTO.error(5, "Room is not AVAILABLE for reservation");
            }
        }

        if (effectiveGuests > room.getCapacity()) {
            return ApiResponseDTO.error(2, "numberOfGuests exceeds room capacity of " + room.getCapacity());
        }

        // Apply updates
        if (dto.getGuestName() != null)  reservation.setGuestName(dto.getGuestName().trim());
        if (dto.getGuestEmail() != null) reservation.setGuestEmail(dto.getGuestEmail().trim());
        if (dto.getGuestPhone() != null) reservation.setGuestPhone(dto.getGuestPhone().trim());
        reservation.setCheckInDate(effectiveCheckIn);
        reservation.setCheckOutDate(effectiveCheckOut);
        reservation.setNumberOfGuests(effectiveGuests);
        reservation.setRoomId(effectiveRoomId);
        reservation.setTotalAmount(computeTotalAmount(room.getPricePerNight(), effectiveCheckIn, effectiveCheckOut));

        Reservation saved = reservationRepository.save(reservation);
        return ApiResponseDTO.ok("Reservation updated successfully", toDTO(saved));
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponseDTO getReservationById(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId).orElse(null);
        if (reservation == null) {
            return ApiResponseDTO.error(4, "Reservation is not found");
        }
        return ApiResponseDTO.ok("Successful", toDetailDTO(reservation));
    }

    @Override
    @Transactional
    public ApiResponseDTO cancelReservation(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId).orElse(null);
        if (reservation == null) {
            return ApiResponseDTO.error(4, "Reservation is not found");
        }
        // Soft operation: set status to CANCELLED
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservationRepository.save(reservation);
        return ApiResponseDTO.ok("Reservation cancelled successfully", null);
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponseDTO getReservationList(Integer page, Integer size, String guestName, String status) {
        int pageIndex = (page == null) ? 0 : page;
        int pageSize = (size == null) ? 10 : size;

        if (pageIndex < 0 || pageSize < 1 || pageSize > 100) {
            return ApiResponseDTO.error(2, "Invalid pagination: page must be >= 0 and size between 1 and 100");
        }

        ReservationStatus parsedStatus = null;
        if (status != null && !status.isBlank()) {
            parsedStatus = parseStatus(status);
            if (parsedStatus == null) {
                return ApiResponseDTO.error(2, "Invalid status filter: must be CONFIRMED, CHECKED_IN, CHECKED_OUT, or CANCELLED");
            }
        }

        String searchName = (guestName == null || guestName.isBlank()) ? null : guestName.trim();
        Pageable pageable = PageRequest.of(pageIndex, pageSize);
        Page<Reservation> resultPage = reservationRepository.search(searchName, parsedStatus, pageable);

        List<ReservationDetailDTO> detailList = resultPage.getContent().stream()
                .map(this::toDetailDTO)
                .toList();

        PageDTO pageDTO = new PageDTO(
                resultPage.getSize(),
                resultPage.getNumber(),
                resultPage.getTotalPages(),
                resultPage.getTotalElements(),
                resultPage.isFirst(),
                resultPage.isLast(),
                detailList
        );

        return ApiResponseDTO.ok("Successful", pageDTO);
    }

    private RoomDTO fetchRoom(Long roomId) {
        try {
            ApiResponseDTO res = roomClient.getRoomById(roomId);
            if (res == null || res.getStatus() != 1 || res.getData() == null) {
                return null;
            }
            return objectMapper.convertValue(res.getData(), RoomDTO.class);
        } catch (FeignException.NotFound ex) {
            return null;
        }
    }

    private BigDecimal computeTotalAmount(BigDecimal pricePerNight, LocalDate checkIn, LocalDate checkOut) {
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        if (nights < 1) nights = 1;
        return pricePerNight.multiply(BigDecimal.valueOf(nights));
    }

    private ReservationDTO toDTO(Reservation r) {
        ReservationDTO dto = new ReservationDTO();
        dto.setReservationId(r.getReservationId());
        dto.setGuestName(r.getGuestName());
        dto.setGuestEmail(r.getGuestEmail());
        dto.setGuestPhone(r.getGuestPhone());
        dto.setCheckInDate(r.getCheckInDate());
        dto.setCheckOutDate(r.getCheckOutDate());
        dto.setNumberOfGuests(r.getNumberOfGuests());
        dto.setTotalAmount(r.getTotalAmount());
        dto.setStatus(r.getStatus().name());
        dto.setRoomId(r.getRoomId());
        return dto;
    }

    private ReservationDetailDTO toDetailDTO(Reservation r) {
        ReservationDetailDTO dto = new ReservationDetailDTO();
        dto.setReservationId(r.getReservationId());
        dto.setGuestName(r.getGuestName());
        dto.setGuestEmail(r.getGuestEmail());
        dto.setGuestPhone(r.getGuestPhone());
        dto.setCheckInDate(r.getCheckInDate());
        dto.setCheckOutDate(r.getCheckOutDate());
        dto.setNumberOfGuests(r.getNumberOfGuests());
        dto.setTotalAmount(r.getTotalAmount());
        dto.setStatus(r.getStatus().name());
        dto.setRoom(fetchRoom(r.getRoomId()));
        return dto;
    }

    private ReservationStatus parseStatus(String statusStr) {
        if (statusStr == null || statusStr.isBlank()) return null;
        try {
            return ReservationStatus.valueOf(statusStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
