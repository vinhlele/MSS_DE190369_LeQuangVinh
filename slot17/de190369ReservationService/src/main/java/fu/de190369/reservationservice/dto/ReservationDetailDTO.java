package fu.de190369.reservationservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ReservationDetailDTO {

    private Long reservationId;
    private String guestName;
    private String guestEmail;
    private String guestPhone;

    @JsonFormat(pattern = "dd/MM/yyyy")
    private LocalDate checkInDate;

    @JsonFormat(pattern = "dd/MM/yyyy")
    private LocalDate checkOutDate;

    private Integer numberOfGuests;
    private BigDecimal totalAmount;
    private String status;

    private RoomDTO room;

    public ReservationDetailDTO() {
    }

    public Long getReservationId() { return reservationId; }
    public void setReservationId(Long reservationId) { this.reservationId = reservationId; }

    public String getGuestName() { return guestName; }
    public void setGuestName(String guestName) { this.guestName = guestName; }

    public String getGuestEmail() { return guestEmail; }
    public void setGuestEmail(String guestEmail) { this.guestEmail = guestEmail; }

    public String getGuestPhone() { return guestPhone; }
    public void setGuestPhone(String guestPhone) { this.guestPhone = guestPhone; }

    public LocalDate getCheckInDate() { return checkInDate; }
    public void setCheckInDate(LocalDate checkInDate) { this.checkInDate = checkInDate; }

    public LocalDate getCheckOutDate() { return checkOutDate; }
    public void setCheckOutDate(LocalDate checkOutDate) { this.checkOutDate = checkOutDate; }

    public Integer getNumberOfGuests() { return numberOfGuests; }
    public void setNumberOfGuests(Integer numberOfGuests) { this.numberOfGuests = numberOfGuests; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public RoomDTO getRoom() { return room; }
    public void setRoom(RoomDTO room) { this.room = room; }
}
