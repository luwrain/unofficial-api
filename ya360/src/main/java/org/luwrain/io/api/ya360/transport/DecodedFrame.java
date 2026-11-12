package org.luwrain.io.api.ya360.transport;

import java.util.List;

/**
 * A decoded binary frame.
 */
public final class DecodedFrame
{
    private final int frameType;
    private final List<Object> headerElements;
    private final byte[] jsonBody;

    public DecodedFrame(int frameType, List<Object> headerElements, byte[] jsonBody)
    {
        this.frameType = frameType;
        this.headerElements = List.copyOf(headerElements);
        this.jsonBody = jsonBody;
    }

    public int frameType()
    {
        return frameType;
    }

    public List<Object> headerElements()
    {
        return headerElements;
    }

    /**
     * @return raw JSON body bytes, or empty array when absent
     */
    public byte[] jsonBody()
    {
        return jsonBody;
    }

    public boolean hasJsonBody()
    {
        return jsonBody.length > 0;
    }
}

