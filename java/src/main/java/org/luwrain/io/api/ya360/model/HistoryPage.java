package org.luwrain.io.api.ya360.model;

import java.util.List;

/**
 * Immutable history page for one chat.
 */
public final class HistoryPage
{
    private final String chatId;
    private final List<Message> messages;
    private final String nextBefore;
    private final boolean hasMore;

    public HistoryPage(String chatId, List<Message> messages, String nextBefore, boolean hasMore)
    {
        this.chatId = chatId;
        this.messages = messages == null ? List.of() : List.copyOf(messages);
        this.nextBefore = nextBefore;
        this.hasMore = hasMore;
    }

    public String chatId()
    {
        return chatId;
    }

    public List<Message> messages()
    {
        return messages;
    }

    public String nextBefore()
    {
        return nextBefore;
    }

    public boolean hasMore()
    {
        return hasMore;
    }
}

