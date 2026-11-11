package org.luwrain.io.api.ya360.model;

/**
 * Immutable normalized chat list entry.
 */
public final class Chat
{
    private final String chatId;
    private final String name;
    private final String kind;
    private final String lastActivityIso;
    private final String lastActivityMcs;
    private final int unreadCount;
    private final boolean unread;
    private final boolean muted;
    private final Message lastMessage;

    public Chat(String chatId, String name, String kind, String lastActivityIso, String lastActivityMcs,
                int unreadCount, boolean unread, boolean muted, Message lastMessage)
    {
        this.chatId = chatId;
        this.name = name;
        this.kind = kind;
        this.lastActivityIso = lastActivityIso;
        this.lastActivityMcs = lastActivityMcs;
        this.unreadCount = unreadCount;
        this.unread = unread;
        this.muted = muted;
        this.lastMessage = lastMessage;
    }

    public String chatId()
    {
        return chatId;
    }

    public String name()
    {
        return name;
    }

    public String kind()
    {
        return kind;
    }

    public String lastActivityIso()
    {
        return lastActivityIso;
    }

    public String lastActivityMcs()
    {
        return lastActivityMcs;
    }

    public int unreadCount()
    {
        return unreadCount;
    }

    public boolean unread()
    {
        return unread;
    }

    public boolean muted()
    {
        return muted;
    }

    public Message lastMessage()
    {
        return lastMessage;
    }
}

