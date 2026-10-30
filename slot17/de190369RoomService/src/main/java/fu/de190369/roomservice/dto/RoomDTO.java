package fu.de190369.roomservice.dto;

import fu.de190369.roomservice.common.RoomConstants;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public class RoomDTO {

    private Long roomId;

    @NotBlank(message = "roomNumber is required")
    @Size(max = 20, message = "roomNumber must be at most 20 characters")
    private String roomNumber;

    @NotBlank(message = "roomType is required")
    @Pattern(regexp = RoomConstants.ROOM_TYPE_REGEX, message = "roomType must be SINGLE, DOUBLE, SUITE, or DELUXE")
    private String roomType;

    @NotNull(message = "pricePerNight is required")
    @DecimalMin(value = "0.01", message = "pricePerNight must be greater than 0")
    private BigDecimal pricePerNight;

    @NotNull(message = "capacity is required")
    @Min(value = 1, message = "capacity must be between 1 and 10")
    @Max(value = 10, message = "capacity must be between 1 and 10")
    private Integer capacity;

    @NotNull(message = "floor is required")
    @Min(value = 1, message = "floor must be at least 1")
    private Integer floor;

    @Pattern(regexp = RoomConstants.ROOM_STATUS_REGEX, message = "status must be AVAILABLE, OCCUPIED, or MAINTENANCE")
    private String status;

    public RoomDTO() {
    }

    public RoomDTO(Long roomId, String roomNumber, String roomType, BigDecimal pricePerNight, Integer capacity, Integer floor, String status) {
        this.roomId = roomId;
        this.roomNumber = roomNumber;
        this.roomType = roomType;
        this.pricePerNight = pricePerNight;
        this.capacity = capacity;
        this.floor = floor;
        this.status = status;
    }

    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }

    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }

    public String getRoomType() { return roomType; }
    public void setRoomType(String roomType) { this.roomType = roomType; }

    public BigDecimal getPricePerNight() { return pricePerNight; }
    public void setPricePerNight(BigDecimal pricePerNight) { this.pricePerNight = pricePerNight; }

    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    public Integer getFloor() { return floor; }
    public void setFloor(Integer floor) { this.floor = floor; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
