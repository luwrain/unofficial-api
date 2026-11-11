package org.luwrain.io.api.ya360.model;

import java.util.List;

/**
 * Immutable normalized message.
 */
public final class Message
{
    private final String id;
    private final String chatId;
    private final String timestampIso;
    private final String timestampMcs;
    private final Integer seqNo;
    private final MessageSender from;
    private final String kind;
    private final String text;
    private final List<AttachmentRef> attachments;
    private final MessageContext context;
    private final boolean edited;
    private final String editedAtIso;
    private final boolean deleted;

    public Message(String id, String chatId, String timestampIso, String timestampMcs, Integer seqNo,
                   MessageSender from, String kind, String text, List<AttachmentRef> attachments,
                   MessageContext context, boolean edited, String editedAtIso, boolean deleted)
    {
        this.id = id;
        this.chatId = chatId;
        this.timestampIso = timestampIso;
        this.timestampMcs = timestampMcs;
        this.seqNo = seqNo;
        this.from = from;
        this.kind = kind;
        this.text = text;
        this.attachments = attachments == null ? List.of() : List.copyOf(attachments);
        this.context = context;
        this.edited = edited;
        this.editedAtIso = editedAtIso;
        this.deleted = deleted;
    }

    public String id()
    {
        return id;
    }

    public String chatId()
    {
        return chatId;
    }

    public String timestampIso()
    {
        return timestampIso;
    }

    public String timestampMcs()
    {
        return timestampMcs;
    }

    public Integer seqNo()
    {
        return seqNo;
    }

    public MessageSender from()
    {
        return from;
    }

    public String kind()
    {
        return kind;
    }

    public String text()
    {
        return text;
    }

    public List<AttachmentRef> attachments()
    {
        return attachments;
    }

    public MessageContext context()
    {
        return context;
    }

    public boolean edited()
    {
        return edited;
    }

    public String editedAtIso()
    {
        return editedAtIso;
    }

    public boolean deleted()
    {
        return deleted;
    }
}

