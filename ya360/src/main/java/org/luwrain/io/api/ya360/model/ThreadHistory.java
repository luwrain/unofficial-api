package org.luwrain.io.api.ya360.model;

import java.util.List;

/**
 * Immutable thread history page.
 */
public final class ThreadHistory
{
    private final String threadId;
    private final boolean empty;
    private final List<Message> messages;
    private final Message parentMessage;
    private final String nextBefore;
    private final boolean hasMore;

    public ThreadHistory(String threadId, boolean empty, List<Message> messages,
                         Message parentMessage, String nextBefore, boolean hasMore)
    {
        this.threadId = threadId;
        this.empty = empty;
        this.messages = messages == null ? List.of() : List.copyOf(messages);
        this.parentMessage = parentMessage;
        this.nextBefore = nextBefore;
        this.hasMore = hasMore;
    }

    public String threadId()
    {
        return threadId;
    }

    public boolean empty()
    {
        return empty;
    }

    public List<Message> messages()
    {
        return messages;
    }

    public Message parentMessage()
    {
        return parentMessage;
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

