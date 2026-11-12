package org.luwrain.io.api.ya360;

/**
 * Protocol violation: undecodable frame, missing Status, or malformed response.
 */
public final class Ya360ProtocolException extends Ya360Exception
{
    public Ya360ProtocolException(String message)
    {
        super("protocol", null, null, false, message);
    }

    public Ya360ProtocolException(String message, Throwable cause)
    {
        super("protocol", null, null, false, message, cause);
    }
}

