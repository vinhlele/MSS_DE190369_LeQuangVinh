package fu.de190369.reservationservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import fu.de190369.reservationservice.common.ReservationConstants;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ReservationDTO {

    private Long reservationId;

    @NotBlank(message = "guestName is required")
    @Size(max = 100, message = "guestName must be at most 100 characters")
    private String guestName;

    @NotBlank(message = "guestEmail is required")
    @Email(message = "guestEmail is invalid")
    @Size(max = 100, message = "guestEmail must be at most 100 characters")
    private String guestEmail;

    @NotBlank(message = "guestPhone is required")
    @Size(max = 20, message = "guestPhone must be at most 20 characters")
    private String guestPhone;

    @NotNull(message = "checkInDate is required")
    @JsonFormat(pattern = "dd/MM/yyyy")
    private LocalDate checkInDate;

    @NotNull(message = "checkOutDate is required")
    @JsonFormat(pattern = "dd/MM/yyyy")
    private LocalDate checkOutDate;

    @NotNull(message = "numberOfGuests is required")
    @Min(value = 1, message = "numberOfGuests must be between 1 and 10")
    @Max(value = 10, message = "numberOfGuests must be between 1 and 10")
    private Integer numberOfGuests;

    private BigDecimal totalAmount;

    @Pattern(regexp = ReservationConstants.STATUS_REGEX, message = "status must be CONFIRMED, CHECKED_IN, CHECKED_OUT, or CANCELLED")
    private String status;

    @NotNull(message = "roomId is required")
    private Long roomId;

    public ReservationDTO() {
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

    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }
}
