package org.luwrain.io.api.ya360.model;

import java.util.List;

/**
 * Immutable context information attached to a message.
 */
public final class MessageContext
{
    private final boolean reply;
    private final List<MessageRef> refs;
    private final List<String> quotes;

    public MessageContext(boolean reply, List<MessageRef> refs, List<String> quotes)
    {
        this.reply = reply;
        this.refs = refs == null ? List.of() : List.copyOf(refs);
        this.quotes = quotes == null ? List.of() : List.copyOf(quotes);
    }

    public boolean reply()
    {
        return reply;
    }

    public List<MessageRef> refs()
    {
        return refs;
    }

    public List<String> quotes()
    {
        return quotes;
    }
}

