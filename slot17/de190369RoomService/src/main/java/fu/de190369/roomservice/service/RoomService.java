package fu.de190369.roomservice.service;

import fu.de190369.roomservice.dto.ApiResponseDTO;
import fu.de190369.roomservice.dto.RoomDTO;

public interface RoomService {
    ApiResponseDTO createRoom(RoomDTO dto);
    ApiResponseDTO updateRoom(Long roomId, RoomDTO dto);
    ApiResponseDTO getRoomById(Long roomId);
    ApiResponseDTO deleteRoom(Long roomId);
    ApiResponseDTO getRoomList(Integer page, Integer size, String roomType, String status);
}
