package org.luwrain.io.api.ya360;

/**
 * Application layer error reported as a non-zero top level Status in a DATA frame.
 */
public final class Ya360ApplicationException extends Ya360Exception
{
    public Ya360ApplicationException(Integer code, String codeName, boolean retriable, String message)
    {
        super("application", code, codeName, retriable, message);
    }

    public Ya360ApplicationException(Integer code, String codeName, boolean retriable, String message, Throwable cause)
    {
        super("application", code, codeName, retriable, message, cause);
    }
}

