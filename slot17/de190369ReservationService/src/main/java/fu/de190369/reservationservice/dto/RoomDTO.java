package fu.de190369.reservationservice.dto;

import java.math.BigDecimal;

/**
 * Local copy of RoomDTO fetched via OpenFeign from RoomService.
 */
public class RoomDTO {

    private Long roomId;
    private String roomNumber;
    private String roomType;
    private BigDecimal pricePerNight;
    private Integer capacity;
    private Integer floor;
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
