package org.luwrain.io.api.ya360.protocol;

/**
 * ProxyStatus transport error codes.
 */
public final class TransportErrorCodes
{
    public static final int SUCCESS = 0;
    public static final int PROTOCOL_ERROR = 1;
    public static final int BACKEND_CALL_ERROR = 2;
    public static final int INTERNAL_ERROR = 3;
    public static final int CORRUPTED_DATA_HEADER = 4;
    public static final int BACKEND_NOT_FOUND = 5;
    public static final int SERVICE_UNAVAILABLE = 6;
    public static final int TOO_MANY_REQUESTS = 7;
    public static final int FRAME_TOO_LARGE = 8;

    private TransportErrorCodes()
    {
    }

    public static boolean retriable(int code)
    {
        return code == SERVICE_UNAVAILABLE || code == TOO_MANY_REQUESTS;
    }

    public static String name(int code)
    {
        return switch (code)
        {
            case SUCCESS -> "SUCCESS";
            case PROTOCOL_ERROR -> "PROTOCOL_ERROR";
            case BACKEND_CALL_ERROR -> "BACKEND_CALL_ERROR";
            case INTERNAL_ERROR -> "INTERNAL_ERROR";
            case CORRUPTED_DATA_HEADER -> "CORRUPTED_DATA_HEADER";
            case BACKEND_NOT_FOUND -> "BACKEND_NOT_FOUND";
            case SERVICE_UNAVAILABLE -> "SERVICE_UNAVAILABLE";
            case TOO_MANY_REQUESTS -> "TOO_MANY_REQUESTS";
            case FRAME_TOO_LARGE -> "FRAME_TOO_LARGE";
            default -> "UNKNOWN(" + code + ")";
        };
    }
}

