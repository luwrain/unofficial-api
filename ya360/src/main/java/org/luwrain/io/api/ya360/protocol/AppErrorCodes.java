package org.luwrain.io.api.ya360.protocol;

/**
 * Application layer Status codes from DATA responses (excluding push).
 */
public final class AppErrorCodes
{
    public static final int SUCCESS = 0;
    public static final int INTERNAL_ERROR = 1;
    public static final int ACCESS_DENIED = 2;
    public static final int MISCONFIGURATION = 3;
    public static final int ENTITY_NOT_FOUND = 4;
    public static final int OVERLOAD = 5;
    public static final int UNAUTHORIZED = 6;

    private AppErrorCodes()
    {
    }

    public static boolean retriable(int code)
    {
        return code == OVERLOAD;
    }

    public static String name(int code)
    {
        return switch (code)
        {
            case SUCCESS -> "SUCCESS";
            case INTERNAL_ERROR -> "INTERNAL_ERROR";
            case ACCESS_DENIED -> "ACCESS_DENIED";
            case MISCONFIGURATION -> "MISCONFIGURATION";
            case ENTITY_NOT_FOUND -> "ENTITY_NOT_FOUND";
            case OVERLOAD -> "OVERLOAD";
            case UNAUTHORIZED -> "UNAUTHORIZED";
            default -> "UNKNOWN(" + code + ")";
        };
    }
}

