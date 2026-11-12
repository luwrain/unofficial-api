package org.luwrain.io.api.ya360.model;

/**
 * Immutable message sender information.
 */
public final class MessageSender
{
    private final String guid;
    private final String name;

    public MessageSender(String guid, String name)
    {
        this.guid = guid;
        this.name = name;
    }

    public String guid()
    {
        return guid;
    }

    public String name()
    {
        return name;
    }
}

