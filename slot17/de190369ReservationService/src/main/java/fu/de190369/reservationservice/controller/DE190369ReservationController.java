package fu.de190369.reservationservice.controller;

import fu.de190369.reservationservice.dto.ApiResponseDTO;
import fu.de190369.reservationservice.dto.ReservationDTO;
import fu.de190369.reservationservice.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reservations")
public class DE190369ReservationController {

    private final ReservationService reservationService;

    public DE190369ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    public ResponseEntity<ApiResponseDTO> createReservation(@Valid @RequestBody ReservationDTO dto) {
        ApiResponseDTO response = reservationService.createReservation(dto);
        if (response.getStatus() == 1) {
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } else if (response.getStatus() == 2) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(response); // HTTP 406
        } else if (response.getStatus() == 4) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);      // HTTP 404
        } else if (response.getStatus() == 5) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);    // HTTP 400
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @PutMapping("/{reservationId}")
    public ResponseEntity<ApiResponseDTO> updateReservation(@PathVariable("reservationId") Long reservationId,
                                                             @RequestBody ReservationDTO dto) {
        ApiResponseDTO response = reservationService.updateReservation(reservationId, dto);
        if (response.getStatus() == 1) {
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } else if (response.getStatus() == 2) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(response); // HTTP 406
        } else if (response.getStatus() == 4) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);      // HTTP 404
        } else if (response.getStatus() == 5) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);    // HTTP 400
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @GetMapping("/{reservationId}")
    public ResponseEntity<ApiResponseDTO> getReservationById(@PathVariable("reservationId") Long reservationId) {
        ApiResponseDTO response = reservationService.getReservationById(reservationId);
        if (response.getStatus() == 1) {
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } else if (response.getStatus() == 4) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);      // HTTP 404
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @DeleteMapping("/{reservationId}")
    public ResponseEntity<ApiResponseDTO> cancelReservation(@PathVariable("reservationId") Long reservationId) {
        ApiResponseDTO response = reservationService.cancelReservation(reservationId);
        if (response.getStatus() == 1) {
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } else if (response.getStatus() == 4) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);      // HTTP 404
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @GetMapping
    public ResponseEntity<ApiResponseDTO> getReservationList(
            @RequestParam(name = "page", required = false, defaultValue = "0") Integer page,
            @RequestParam(name = "size", required = false, defaultValue = "10") Integer size,
            @RequestParam(name = "guestName", required = false) String guestName,
            @RequestParam(name = "status", required = false) String status) {
        ApiResponseDTO response = reservationService.getReservationList(page, size, guestName, status);
        if (response.getStatus() == 1) {
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } else if (response.getStatus() == 2) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(response); // HTTP 406
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
