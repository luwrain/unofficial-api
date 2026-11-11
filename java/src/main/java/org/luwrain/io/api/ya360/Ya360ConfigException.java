package org.luwrain.io.api.ya360;

/**
 * Thrown when the library configuration cannot be loaded or is invalid.
 */
public final class Ya360ConfigException extends Exception
{
    public Ya360ConfigException(String message)
    {
        super(message);
    }

    public Ya360ConfigException(String message, Throwable cause)
    {
        super(message, cause);
    }
}

