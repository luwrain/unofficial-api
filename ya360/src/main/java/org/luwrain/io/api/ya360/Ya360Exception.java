package org.luwrain.io.api.ya360;

/**
 * Base unchecked exception for the library.
 *
 * <p>The {@code layer} property is one of: "transport", "application", "push",
 * "auth" or "protocol".</p>
 */
public class Ya360Exception extends RuntimeException
{
    private final String layer;
    private final Integer code;
    private final String codeName;
    private final boolean retriable;

    protected Ya360Exception(String layer, Integer code, String codeName, boolean retriable, String message)
    {
        super(message);
        this.layer = layer;
        this.code = code;
        this.codeName = codeName;
        this.retriable = retriable;
    }

    protected Ya360Exception(String layer, Integer code, String codeName, boolean retriable, String message, Throwable cause)
    {
        super(message, cause);
        this.layer = layer;
        this.code = code;
        this.codeName = codeName;
        this.retriable = retriable;
    }

    public String layer()
    {
        return layer;
    }

    public Integer code()
    {
        return code;
    }

    public String codeName()
    {
        return codeName;
    }

    public boolean retriable()
    {
        return retriable;
    }
}

