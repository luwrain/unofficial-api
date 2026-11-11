package org.luwrain.io.api.ya360.model;

/**
 * Immutable push response.
 */
public final class PushResult
{
    private final int status;
    private final String statusName;
    private final boolean committed;
    private final boolean duplicate;
    private final Long timestampMcs;
    private final Long seqNo;
    private final Long version;
    private final Integer rateLimitWaitFor;

    public PushResult(int status, String statusName, boolean committed, boolean duplicate,
                      Long timestampMcs, Long seqNo, Long version, Integer rateLimitWaitFor)
    {
        this.status = status;
        this.statusName = statusName;
        this.committed = committed;
        this.duplicate = duplicate;
        this.timestampMcs = timestampMcs;
        this.seqNo = seqNo;
        this.version = version;
        this.rateLimitWaitFor = rateLimitWaitFor;
    }

    public int status()
    {
        return status;
    }

    public String statusName()
    {
        return statusName;
    }

    public boolean committed()
    {
        return committed;
    }

    public boolean duplicate()
    {
        return duplicate;
    }

    public Long timestampMcs()
    {
        return timestampMcs;
    }

    public Long seqNo()
    {
        return seqNo;
    }

    public Long version()
    {
        return version;
    }

    public Integer rateLimitWaitFor()
    {
        return rateLimitWaitFor;
    }
}

