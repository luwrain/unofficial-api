package org.luwrain.io.api.ya360.model;

/**
 * Immutable reference to another message.
 */
public final class MessageRef
{
    private final String chatId;
    private final String timestampMcs;
    private final String timestampIso;

    public MessageRef(String chatId, String timestampMcs, String timestampIso)
    {
        this.chatId = chatId;
        this.timestampMcs = timestampMcs;
        this.timestampIso = timestampIso;
    }

    public String chatId()
    {
        return chatId;
    }

    public String timestampMcs()
    {
        return timestampMcs;
    }

    public String timestampIso()
    {
        return timestampIso;
    }
}

