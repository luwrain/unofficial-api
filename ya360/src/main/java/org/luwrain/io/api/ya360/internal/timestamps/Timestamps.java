package org.luwrain.io.api.ya360.internal.timestamps;

import org.luwrain.io.api.ya360.Ya360ProtocolException;

import java.time.Instant;
import java.time.format.DateTimeParseException;

/**
 * Timestamp helpers preserving microsecond strings in the public model.
 */
public final class Timestamps
{
    private Timestamps()
    {
    }

    /**
     * Converts a microsecond timestamp string to ISO-8601 with millisecond precision.
     */
    public static String microsToIso(String micros)
    {
        if (micros == null)
        {
            return null;
        }
        try
        {
            return microsToIso(Long.parseLong(micros));
        }
        catch (NumberFormatException e)
        {
            throw new Ya360ProtocolException("Invalid microsecond timestamp: " + micros, e);
        }
    }

    /**
     * Converts microsecond timestamp to ISO-8601 with millisecond precision.
     */
    public static String microsToIso(long micros)
    {
        final long seconds = Math.floorDiv(micros, 1_000_000L);
        final long microsRemainder = Math.floorMod(micros, 1_000_000L);
        final long millis = microsRemainder / 1_000L;
        return Instant.ofEpochSecond(seconds, millis * 1_000_000L).toString();
    }

    /**
     * Converts an ISO-8601 timestamp to epoch milliseconds.
     */
    public static long isoToMillis(String iso)
    {
        if (iso == null)
        {
            throw new Ya360ProtocolException("ISO timestamp is null");
        }
        try
        {
            return Instant.parse(iso).toEpochMilli();
        }
        catch (DateTimeParseException e)
        {
            throw new Ya360ProtocolException("Invalid ISO timestamp: " + iso, e);
        }
    }
}

