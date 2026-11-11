package org.luwrain.io.api.ya360.model;

/**
 * Immutable attachment reference read from a message content field.
 */
public final class AttachmentRef
{
    private final String kind;
    private final String fileId;
    private final String name;
    private final Long size;
    private final String source;
    private final Integer width;
    private final Integer height;
    private final Boolean animated;
    private final Integer duration;

    public AttachmentRef(String kind, String fileId, String name, Long size, String source,
                         Integer width, Integer height, Boolean animated, Integer duration)
    {
        this.kind = kind;
        this.fileId = fileId;
        this.name = name;
        this.size = size;
        this.source = source;
        this.width = width;
        this.height = height;
        this.animated = animated;
        this.duration = duration;
    }

    public String kind()
    {
        return kind;
    }

    public String fileId()
    {
        return fileId;
    }

    public String name()
    {
        return name;
    }

    public Long size()
    {
        return size;
    }

    public String source()
    {
        return source;
    }

    public Integer width()
    {
        return width;
    }

    public Integer height()
    {
        return height;
    }

    public Boolean animated()
    {
        return animated;
    }

    public Integer duration()
    {
        return duration;
    }
}

