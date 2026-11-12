package org.luwrain.io.api.ya360;

/**
 * Push commit status error. The numeric commit status is available via {@code status()}.
 */
public final class Ya360PushException extends Ya360Exception
{
    public Ya360PushException(int status, String statusName, String message)
    {
        super("push", status, statusName, false, message);
    }

    public int status()
    {
        return code() == null ? 0 : code();
    }
}

