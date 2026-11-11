package org.luwrain.io.api.ya360.transport;

import org.luwrain.io.api.ya360.Ya360ProtocolException;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Encodes and decodes the fixed xiva MessagePack header around a JSON body.
 *
 * <p>The decoder advances by MessagePack element lengths, never by searching for the
 * data section marker byte {@code 0x05}. This matters because a positive fixint seq
 * equal to 5 also encodes as {@code 0x05}.</p>
 */
public final class FrameCodec
{
    private static final int DATA_SECTION_MARKER = 0x05;
    private static final int DATA_SECTION_LENGTH = 12;

    private FrameCodec()
    {
    }

    /**
     * Encodes a Data frame.
     */
    public static byte[] encodeDataFrame(int serviceIndex, int seq, String method, byte[] jsonBody)
    {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(FrameType.DATA);
        writeArrayHeader(out, 3);
        writeUint(out, serviceIndex);
        writeUint(out, seq);
        writeString(out, method);
        out.write(DATA_SECTION_MARKER);
        for (int i = 0; i < DATA_SECTION_LENGTH - 1; i++)
        {
            out.write(0);
        }
        out.writeBytes(jsonBody);
        return out.toByteArray();
    }

    /**
     * Decodes a binary frame.
     *
     * @throws Ya360ProtocolException when the frame is malformed
     */
    public static DecodedFrame decodeFrame(byte[] frame)
    {
        if (frame == null || frame.length == 0)
        {
            throw new Ya360ProtocolException("Empty frame");
        }
        final int frameType = frame[0] & 0xff;
        if (frameType != FrameType.DATA && frameType != FrameType.PROXY_STATUS && frameType != FrameType.PUSH)
        {
            throw new Ya360ProtocolException("Unknown frame type: " + frameType);
        }
        if (frame.length < 2)
        {
            throw new Ya360ProtocolException("Frame is too short to contain a header");
        }
        final int arrayTag = frame[1] & 0xff;
        if ((arrayTag & 0xf0) != 0x90)
        {
            throw new Ya360ProtocolException("Expected fixarray header, got: " + arrayTag);
        }
        final int arity = arrayTag & 0x0f;

        int offset = 2;
        final List<Object> headerElements = new ArrayList<>(arity);
        for (int i = 0; i < arity; i++)
        {
            final Element element = decodeElement(frame, offset);
            offset = element.nextOffset;
            headerElements.add(element.value);
        }

        final int jsonStart = offset + DATA_SECTION_LENGTH;
        if (jsonStart > frame.length)
        {
            throw new Ya360ProtocolException("Frame is too short: missing data section");
        }
        final byte[] jsonBody = new byte[frame.length - jsonStart];
        System.arraycopy(frame, jsonStart, jsonBody, 0, jsonBody.length);
        return new DecodedFrame(frameType, headerElements, jsonBody);
    }

    private static Element decodeElement(byte[] frame, int offset)
    {
        if (offset >= frame.length)
        {
            throw new Ya360ProtocolException("Truncated header");
        }
        final int tag = frame[offset] & 0xff;
        if (tag < 0x80)
        {
            return new Element((long) tag, offset + 1);
        }
        if (tag == 0xcc)
        {
            ensure(frame, offset, 1);
            return new Element((long) (frame[offset + 1] & 0xff), offset + 2);
        }
        if (tag == 0xcd)
        {
            ensure(frame, offset, 2);
            final long value = (frame[offset + 1] & 0xff) << 8 | (frame[offset + 2] & 0xff);
            return new Element(value, offset + 3);
        }
        if (tag == 0xce)
        {
            ensure(frame, offset, 4);
            final long value = (frame[offset + 1] & 0xffL) << 24
                | (frame[offset + 2] & 0xffL) << 16
                | (frame[offset + 3] & 0xffL) << 8
                | (frame[offset + 4] & 0xffL);
            return new Element(value, offset + 5);
        }
        if ((tag & 0xe0) == 0xa0)
        {
            final int length = tag & 0x1f;
            ensure(frame, offset, length);
            return new Element(new String(frame, offset + 1, length, StandardCharsets.UTF_8), offset + 1 + length);
        }
        if (tag == 0xd9)
        {
            ensure(frame, offset, 1);
            final int length = frame[offset + 1] & 0xff;
            ensure(frame, offset + 1, length);
            return new Element(new String(frame, offset + 2, length, StandardCharsets.UTF_8), offset + 2 + length);
        }
        if (tag == 0xda)
        {
            ensure(frame, offset, 2);
            final int length = (frame[offset + 1] & 0xff) << 8 | (frame[offset + 2] & 0xff);
            ensure(frame, offset + 2, length);
            return new Element(new String(frame, offset + 3, length, StandardCharsets.UTF_8), offset + 3 + length);
        }
        throw new Ya360ProtocolException("Unsupported MessagePack tag in header: " + tag);
    }

    private static void ensure(byte[] frame, int offset, int length)
    {
        if (offset + length >= frame.length)
        {
            throw new Ya360ProtocolException("Truncated header");
        }
    }

    private static void writeArrayHeader(ByteArrayOutputStream out, int length)
    {
        out.write(0x90 | length);
    }

    private static void writeUint(ByteArrayOutputStream out, int value)
    {
        if (value < 0x80)
        {
            out.write(value);
        }
        else if (value < 0x100)
        {
            out.write(0xcc);
            out.write(value);
        }
        else if (value < 0x10000)
        {
            out.write(0xcd);
            out.write((value >>> 8) & 0xff);
            out.write(value & 0xff);
        }
        else
        {
            out.write(0xce);
            final byte[] bytes = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(value).array();
            out.writeBytes(bytes);
        }
    }

    private static void writeString(ByteArrayOutputStream out, String value)
    {
        final byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 0x20)
        {
            out.write(0xa0 | bytes.length);
        }
        else if (bytes.length < 0x100)
        {
            out.write(0xd9);
            out.write(bytes.length);
        }
        else if (bytes.length < 0x10000)
        {
            out.write(0xda);
            out.write((bytes.length >>> 8) & 0xff);
            out.write(bytes.length & 0xff);
        }
        else
        {
            throw new Ya360ProtocolException("Method name is too long");
        }
        out.writeBytes(bytes);
    }

    private record Element(Object value, int nextOffset)
    {
    }
}

