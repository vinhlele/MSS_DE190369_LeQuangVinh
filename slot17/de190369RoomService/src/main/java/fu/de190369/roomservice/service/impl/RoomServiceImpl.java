package fu.de190369.roomservice.service.impl;

import fu.de190369.roomservice.dto.ApiResponseDTO;
import fu.de190369.roomservice.dto.PageDTO;
import fu.de190369.roomservice.dto.RoomDTO;
import fu.de190369.roomservice.entity.Room;
import fu.de190369.roomservice.entity.RoomStatus;
import fu.de190369.roomservice.entity.RoomType;
import fu.de190369.roomservice.repository.RoomRepository;
import fu.de190369.roomservice.service.RoomService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;

    public RoomServiceImpl(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    @Override
    @Transactional
    public ApiResponseDTO createRoom(RoomDTO dto) {
        // Status 3 -> Duplicated room number
        if (roomRepository.existsByRoomNumber(dto.getRoomNumber())) {
            return ApiResponseDTO.error(3, "Room number is duplicated");
        }

        Room room = new Room();
        room.setRoomNumber(dto.getRoomNumber().trim());
        room.setRoomType(RoomType.valueOf(dto.getRoomType().trim().toUpperCase()));
        room.setPricePerNight(dto.getPricePerNight());
        room.setCapacity(dto.getCapacity());
        room.setFloor(dto.getFloor());

        // Default status is AVAILABLE when creating a room
        if (dto.getStatus() == null || dto.getStatus().isBlank()) {
            room.setStatus(RoomStatus.AVAILABLE);
        } else {
            room.setStatus(RoomStatus.valueOf(dto.getStatus().trim().toUpperCase()));
        }

        Room saved = roomRepository.save(room);
        return ApiResponseDTO.of(1, "Room created successfully", toDTO(saved));
    }

    @Override
    @Transactional
    public ApiResponseDTO updateRoom(Long roomId, RoomDTO dto) {
        Room room = roomRepository.findById(roomId).orElse(null);
        if (room == null) {
            return ApiResponseDTO.error(4, "Room is not found");
        }

        // Validate partial inputs and check duplicates
        if (dto.getRoomNumber() != null) {
            String trimmedNumber = dto.getRoomNumber().trim();
            if (trimmedNumber.isBlank() || trimmedNumber.length() > 20) {
                return ApiResponseDTO.error(2, "roomNumber must be between 1 and 20 characters");
            }
            if (roomRepository.existsByRoomNumberAndRoomIdNot(trimmedNumber, roomId)) {
                return ApiResponseDTO.error(3, "Room number is duplicated");
            }
            room.setRoomNumber(trimmedNumber);
        }

        if (dto.getRoomType() != null) {
            RoomType parsedType = parseRoomType(dto.getRoomType());
            if (parsedType == null) {
                return ApiResponseDTO.error(2, "roomType must be SINGLE, DOUBLE, SUITE, or DELUXE");
            }
            room.setRoomType(parsedType);
        }

        if (dto.getPricePerNight() != null) {
            if (dto.getPricePerNight().compareTo(BigDecimal.ZERO) <= 0) {
                return ApiResponseDTO.error(2, "pricePerNight must be greater than 0");
            }
            room.setPricePerNight(dto.getPricePerNight());
        }

        if (dto.getCapacity() != null) {
            if (dto.getCapacity() < 1 || dto.getCapacity() > 10) {
                return ApiResponseDTO.error(2, "capacity must be between 1 and 10");
            }
            room.setCapacity(dto.getCapacity());
        }

        if (dto.getFloor() != null) {
            if (dto.getFloor() < 1) {
                return ApiResponseDTO.error(2, "floor must be at least 1");
            }
            room.setFloor(dto.getFloor());
        }

        if (dto.getStatus() != null) {
            RoomStatus parsedStatus = parseRoomStatus(dto.getStatus());
            if (parsedStatus == null) {
                return ApiResponseDTO.error(2, "status must be AVAILABLE, OCCUPIED, or MAINTENANCE");
            }
            room.setStatus(parsedStatus);
        }

        Room saved = roomRepository.save(room);
        return ApiResponseDTO.ok("Room updated successfully", toDTO(saved));
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponseDTO getRoomById(Long roomId) {
        Room room = roomRepository.findById(roomId).orElse(null);
        if (room == null) {
            return ApiResponseDTO.error(4, "Room is not found");
        }
        return ApiResponseDTO.ok("Successful", toDTO(room));
    }

    @Override
    @Transactional
    public ApiResponseDTO deleteRoom(Long roomId) {
        Room room = roomRepository.findById(roomId).orElse(null);
        if (room == null) {
            return ApiResponseDTO.error(4, "Room is not found");
        }
        // Soft operation: set status to MAINTENANCE
        room.setStatus(RoomStatus.MAINTENANCE);
        roomRepository.save(room);
        return ApiResponseDTO.ok("Room is set to MAINTENANCE successfully", null);
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponseDTO getRoomList(Integer page, Integer size, String roomType, String status) {
        int pageIndex = (page == null) ? 0 : page;
        int pageSize = (size == null) ? 10 : size;

        if (pageIndex < 0 || pageSize < 1 || pageSize > 100) {
            return ApiResponseDTO.error(2, "Invalid pagination: page must be >= 0 and size between 1 and 100");
        }

        RoomType parsedType = null;
        if (roomType != null && !roomType.isBlank()) {
            parsedType = parseRoomType(roomType);
            if (parsedType == null) {
                return ApiResponseDTO.error(2, "Invalid roomType filter: must be SINGLE, DOUBLE, SUITE, or DELUXE");
            }
        }

        RoomStatus parsedStatus = null;
        if (status != null && !status.isBlank()) {
            parsedStatus = parseRoomStatus(status);
            if (parsedStatus == null) {
                return ApiResponseDTO.error(2, "Invalid status filter: must be AVAILABLE, OCCUPIED, or MAINTENANCE");
            }
        }

        Pageable pageable = PageRequest.of(pageIndex, pageSize);
        Page<Room> resultPage = roomRepository.search(parsedType, parsedStatus, pageable);

        List<RoomDTO> dtoList = resultPage.getContent().stream()
                .map(this::toDTO)
                .toList();

        PageDTO pageDTO = new PageDTO(
                resultPage.getSize(),
                resultPage.getNumber(),
                resultPage.getTotalPages(),
                resultPage.getTotalElements(),
                resultPage.isFirst(),
                resultPage.isLast(),
                dtoList
        );

        return ApiResponseDTO.ok("Successful", pageDTO);
    }

    private RoomDTO toDTO(Room room) {
        RoomDTO dto = new RoomDTO();
        dto.setRoomId(room.getRoomId());
        dto.setRoomNumber(room.getRoomNumber());
        dto.setRoomType(room.getRoomType().name());
        dto.setPricePerNight(room.getPricePerNight());
        dto.setCapacity(room.getCapacity());
        dto.setFloor(room.getFloor());
        dto.setStatus(room.getStatus().name());
        return dto;
    }

    private RoomType parseRoomType(String typeStr) {
        if (typeStr == null || typeStr.isBlank()) return null;
        try {
            return RoomType.valueOf(typeStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private RoomStatus parseRoomStatus(String statusStr) {
        if (statusStr == null || statusStr.isBlank()) return null;
        try {
            return RoomStatus.valueOf(statusStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
