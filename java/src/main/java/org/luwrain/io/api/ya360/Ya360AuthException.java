package org.luwrain.io.api.ya360;

/**
 * Authentication failure. In this milestone it is not retryable automatically.
 */
public final class Ya360AuthException extends Ya360Exception
{
    public Ya360AuthException(String message)
    {
        super("auth", null, "AUTH", false, message);
    }

    public Ya360AuthException(String message, Throwable cause)
    {
        super("auth", null, "AUTH", false, message, cause);
    }
}

