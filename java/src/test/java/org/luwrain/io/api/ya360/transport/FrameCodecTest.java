package org.luwrain.io.api.ya360.transport;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FrameCodecTest
{
    @Test
    void encodesWhoamiByteForByte()
    {
        final byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
        final byte[] encoded = FrameCodec.encodeDataFrame(0, 1, "whoami", body);

        final byte[] expected = new byte[]{
            0x01, 0x93, 0x00, 0x01, (byte) 0xa6,
            0x77, 0x68, 0x6f, 0x61, 0x6d, 0x69,
            0x05, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x7b, 0x7d
        };
        assertArrayEquals(expected, encoded);
    }

    @Test
    void decodesSeqFiveWithoutConfusingDataMarker()
    {
        final byte[] body = "{\"RequestId\":\"x\"}".getBytes(StandardCharsets.UTF_8);
        final byte[] encoded = FrameCodec.encodeDataFrame(0, 5, "history", body);

        final DecodedFrame decoded = FrameCodec.decodeFrame(encoded);
        assertEquals(FrameType.DATA, decoded.frameType());
        assertEquals(3, decoded.headerElements().size());
        assertEquals(0L, decoded.headerElements().get(0));
        assertEquals(5L, decoded.headerElements().get(1));
        assertEquals("history", decoded.headerElements().get(2));
        assertArrayEquals(body, decoded.jsonBody());
    }

    @Test
    void encodesLongSeqWithUint16()
    {
        final byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
        final byte[] encoded = FrameCodec.encodeDataFrame(0, 0x0102, "push", body);

        // serviceIndex, seq(0xcd 0x01 0x02), fixstr push
        final byte[] expectedHeader = new byte[]{
            0x01, 0x93, 0x00, (byte) 0xcd, 0x01, 0x02, (byte) 0xa4,
            0x70, 0x75, 0x73, 0x68
        };
        for (int i = 0; i < expectedHeader.length; i++)
        {
            assertEquals(expectedHeader[i] & 0xff, encoded[i] & 0xff, "byte " + i);
        }
    }

    @Test
    void roundTripsProxyStatus()
    {
        final byte[] frame = new byte[]{
            0x02, 0x92, 0x01, 0x06,
            0x05, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
        };
        final DecodedFrame decoded = FrameCodec.decodeFrame(frame);
        assertEquals(FrameType.PROXY_STATUS, decoded.frameType());
        assertEquals(2, decoded.headerElements().size());
        assertEquals(1L, decoded.headerElements().get(0));
        assertEquals(6L, decoded.headerElements().get(1));
        assertFalse(decoded.hasJsonBody());
    }

    @Test
    void roundTripsPushFrameWithLongHeader()
    {
        final byte[] json = "{\"k\":1}".getBytes(StandardCharsets.UTF_8);
        final byte[] encoded = FrameCodec.encodeDataFrame(0, 300, "history", json);
        final DecodedFrame decoded = FrameCodec.decodeFrame(encoded);
        assertEquals(300L, decoded.headerElements().get(1));
        assertEquals("history", decoded.headerElements().get(2));
        assertTrue(decoded.hasJsonBody());
    }
}

