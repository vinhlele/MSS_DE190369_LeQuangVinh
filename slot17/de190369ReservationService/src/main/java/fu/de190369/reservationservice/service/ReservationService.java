package fu.de190369.reservationservice.service;

import fu.de190369.reservationservice.dto.ApiResponseDTO;
import fu.de190369.reservationservice.dto.ReservationDTO;

public interface ReservationService {
    ApiResponseDTO createReservation(ReservationDTO dto);
    ApiResponseDTO updateReservation(Long reservationId, ReservationDTO dto);
    ApiResponseDTO getReservationById(Long reservationId);
    ApiResponseDTO cancelReservation(Long reservationId);
    ApiResponseDTO getReservationList(Integer page, Integer size, String guestName, String status);
}
