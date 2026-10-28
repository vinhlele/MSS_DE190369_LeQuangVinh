package fu.de190369.userservice.common;

public final class UserConstants {

    private UserConstants() {}

    public static final String ACTIVE = "ACTIVE";
    public static final String SUSPENDED = "SUSPENDED";
    public static final String DEACTIVATED = "DEACTIVATED";

    public static final String ROLE_USER = "USER";
    public static final String ROLE_CREATOR = "CREATOR";
    public static final String ROLE_ADMIN = "ADMIN";

    public static final String ROLE_REGEX = "USER|CREATOR|ADMIN";
    public static final String STATUS_REGEX = "ACTIVE|SUSPENDED|DEACTIVATED";
}
