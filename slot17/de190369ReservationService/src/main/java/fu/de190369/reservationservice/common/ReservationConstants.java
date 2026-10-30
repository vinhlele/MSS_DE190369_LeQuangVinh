package fu.de190369.reservationservice.common;

public final class ReservationConstants {

    private ReservationConstants() {
    }

    public static final String CONFIRMED = "CONFIRMED";
    public static final String CHECKED_IN = "CHECKED_IN";
    public static final String CHECKED_OUT = "CHECKED_OUT";
    public static final String CANCELLED = "CANCELLED";

    public static final String STATUS_REGEX = "CONFIRMED|CHECKED_IN|CHECKED_OUT|CANCELLED";

    public static final String ROOM_AVAILABLE = "AVAILABLE";
}
