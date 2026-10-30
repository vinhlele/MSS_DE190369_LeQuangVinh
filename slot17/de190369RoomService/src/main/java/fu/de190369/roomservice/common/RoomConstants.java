package fu.de190369.roomservice.common;

public final class RoomConstants {

    private RoomConstants() {
    }

    public static final String SINGLE = "SINGLE";
    public static final String DOUBLE = "DOUBLE";
    public static final String SUITE = "SUITE";
    public static final String DELUXE = "DELUXE";

    public static final String AVAILABLE = "AVAILABLE";
    public static final String OCCUPIED = "OCCUPIED";
    public static final String MAINTENANCE = "MAINTENANCE";

    public static final String ROOM_TYPE_REGEX = "SINGLE|DOUBLE|SUITE|DELUXE";
    public static final String ROOM_STATUS_REGEX = "AVAILABLE|OCCUPIED|MAINTENANCE";
}
