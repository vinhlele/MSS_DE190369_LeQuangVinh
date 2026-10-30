package fu.de190369.roomservice.controller;

import fu.de190369.roomservice.dto.ApiResponseDTO;
import fu.de190369.roomservice.dto.RoomDTO;
import fu.de190369.roomservice.service.RoomService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rooms")
public class DE190369RoomController {

    private final RoomService roomService;

    public DE190369RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @PostMapping
    public ResponseEntity<ApiResponseDTO> createRoom(@Valid @RequestBody RoomDTO dto) {
        ApiResponseDTO response = roomService.createRoom(dto);
        if (response.getStatus() == 1) {
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } else if (response.getStatus() == 3) {
            return ResponseEntity.status(HttpStatus.IM_USED).body(response); // HTTP 226
        } else if (response.getStatus() == 2) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(response); // HTTP 406
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @PutMapping("/{roomId}")
    public ResponseEntity<ApiResponseDTO> updateRoom(@PathVariable("roomId") Long roomId,
                                                     @RequestBody RoomDTO dto) {
        ApiResponseDTO response = roomService.updateRoom(roomId, dto);
        if (response.getStatus() == 1) {
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } else if (response.getStatus() == 4) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response); // HTTP 404
        } else if (response.getStatus() == 3) {
            return ResponseEntity.status(HttpStatus.IM_USED).body(response); // HTTP 226
        } else if (response.getStatus() == 2) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(response); // HTTP 406
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<ApiResponseDTO> getRoomById(@PathVariable("roomId") Long roomId) {
        ApiResponseDTO response = roomService.getRoomById(roomId);
        if (response.getStatus() == 1) {
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } else if (response.getStatus() == 4) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response); // HTTP 404
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @DeleteMapping("/{roomId}")
    public ResponseEntity<ApiResponseDTO> deleteRoom(@PathVariable("roomId") Long roomId) {
        ApiResponseDTO response = roomService.deleteRoom(roomId);
        if (response.getStatus() == 1) {
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } else if (response.getStatus() == 4) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response); // HTTP 404
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @GetMapping
    public ResponseEntity<ApiResponseDTO> getRoomList(@RequestParam(name = "page", required = false, defaultValue = "0") Integer page,
                                                      @RequestParam(name = "size", required = false, defaultValue = "10") Integer size,
                                                      @RequestParam(name = "roomType", required = false) String roomType,
                                                      @RequestParam(name = "status", required = false) String status) {
        ApiResponseDTO response = roomService.getRoomList(page, size, roomType, status);
        if (response.getStatus() == 1) {
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } else if (response.getStatus() == 2) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(response); // HTTP 406
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
