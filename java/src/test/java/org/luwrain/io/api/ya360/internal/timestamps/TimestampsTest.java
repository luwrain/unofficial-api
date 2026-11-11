package org.luwrain.io.api.ya360.internal.timestamps;

import org.junit.jupiter.api.Test;
import org.luwrain.io.api.ya360.Ya360ProtocolException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TimestampsTest
{
    @Test
    void convertsMicrosToIso()
    {
        assertEquals("2023-11-14T22:13:20.123Z", Timestamps.microsToIso("1700000000123456"));
    }

    @Test
    void rejectsInvalidMicros()
    {
        assertThrows(Ya360ProtocolException.class, () -> Timestamps.microsToIso("abc"));
    }

    @Test
    void convertsIsoToMillis()
    {
        assertEquals(1700000000123L, Timestamps.isoToMillis("2023-11-14T22:13:20.123Z"));
    }
}

