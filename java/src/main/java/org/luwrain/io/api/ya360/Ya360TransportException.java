package org.luwrain.io.api.ya360;

/**
 * Transport layer error reported through a ProxyStatus frame.
 */
public final class Ya360TransportException extends Ya360Exception
{
    public Ya360TransportException(Integer code, String codeName, boolean retriable, String message)
    {
        super("transport", code, codeName, retriable, message);
    }

    public Ya360TransportException(Integer code, String codeName, boolean retriable, String message, Throwable cause)
    {
        super("transport", code, codeName, retriable, message, cause);
    }
}

