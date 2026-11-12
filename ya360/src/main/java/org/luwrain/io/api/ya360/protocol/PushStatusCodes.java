package org.luwrain.io.api.ya360.protocol;

/**
 * Push commit status codes.
 */
public final class PushStatusCodes
{
    public static final int UNCOMMMITED = 0;
    public static final int FULLY_COMMITTED = 1;
    public static final int UNIPROXY_COMMITTED = 2;
    public static final int FAILED = 3;
    public static final int NO_SUCH_CHAT = 4;
    public static final int NOT_LEADING = 5;
    public static final int FOREIGN_PARTITION = 6;
    public static final int SENDER_NOT_IN_CHAT = 7;
    public static final int DUPLICATE = 8;
    public static final int POSTPROC_COMMITTED = 9;
    public static final int KIKIMR_WRITE_FAILED = 10;
    public static final int MESSAGE_NOT_FOUND = 11;
    public static final int DEQUEUED_AFTER_ERROR = 12;
    public static final int BAD_REQUEST = 13;
    public static final int FILESHARE_FAILED = 14;
    public static final int NO_PERMISSION = 15;
    public static final int CONFLICT = 16;
    public static final int NO_SUCH_USER = 17;
    public static final int THROTTLED = 18;
    public static final int BANNED = 19;
    public static final int BLACKLISTED = 20;
    public static final int NOT_FOUND = 21;
    public static final int SPAM_DETECTED = 22;
    public static final int RATE_LIMIT_EXCEEDED = 23;
    public static final int BLOCKED_BY_PRIVACY_SETTINGS = 24;
    public static final int PUSH_UNAUTHORIZED = 25;

    private PushStatusCodes()
    {
    }

    public static boolean success(int code)
    {
        return code == FULLY_COMMITTED || code == DUPLICATE;
    }

    public static boolean duplicate(int code)
    {
        return code == DUPLICATE;
    }

    public static String name(int code)
    {
        return switch (code)
        {
            case UNCOMMMITED -> "UNCOMMMITED";
            case FULLY_COMMITTED -> "FULLY_COMMITTED";
            case UNIPROXY_COMMITTED -> "UNIPROXY_COMMITTED";
            case FAILED -> "FAILED";
            case NO_SUCH_CHAT -> "NO_SUCH_CHAT";
            case NOT_LEADING -> "NOT_LEADING";
            case FOREIGN_PARTITION -> "FOREIGN_PARTITION";
            case SENDER_NOT_IN_CHAT -> "SENDER_NOT_IN_CHAT";
            case DUPLICATE -> "DUPLICATE";
            case POSTPROC_COMMITTED -> "POSTPROC_COMMITTED";
            case KIKIMR_WRITE_FAILED -> "KIKIMR_WRITE_FAILED";
            case MESSAGE_NOT_FOUND -> "MESSAGE_NOT_FOUND";
            case DEQUEUED_AFTER_ERROR -> "DEQUEUED_AFTER_ERROR";
            case BAD_REQUEST -> "BAD_REQUEST";
            case FILESHARE_FAILED -> "FILESHARE_FAILED";
            case NO_PERMISSION -> "NO_PERMISSION";
            case CONFLICT -> "CONFLICT";
            case NO_SUCH_USER -> "NO_SUCH_USER";
            case THROTTLED -> "THROTTLED";
            case BANNED -> "BANNED";
            case BLACKLISTED -> "BLACKLISTED";
            case NOT_FOUND -> "NOT_FOUND";
            case SPAM_DETECTED -> "SPAM_DETECTED";
            case RATE_LIMIT_EXCEEDED -> "RATE_LIMIT_EXCEEDED";
            case BLOCKED_BY_PRIVACY_SETTINGS -> "BLOCKED_BY_PRIVACY_SETTINGS";
            case PUSH_UNAUTHORIZED -> "PUSH_UNAUTHORIZED";
            default -> "UNKNOWN(" + code + ")";
        };
    }
}

