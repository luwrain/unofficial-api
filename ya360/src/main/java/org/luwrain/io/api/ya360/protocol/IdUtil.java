package org.luwrain.io.api.ya360.protocol;

import java.security.SecureRandom;

/**
 * Generates wire identifiers. Package private on purpose; callers use higher level
 * operations that need these values internally.
 */
final class IdUtil
{
    private static final char[] HEX = "0123456789abcdef".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    private IdUtil()
    {
    }

    /**
     * Generates a random xiva session token formatted as {@code xxxx-xxxx-xxxx-xxxx}.
     */
    static String session()
    {
        final byte[] bytes = randomBytes(8);
        final String hex = hex(bytes);
        return hex.substring(0, 4) + "-"
            + hex.substring(4, 8) + "-"
            + hex.substring(8, 12) + "-"
            + hex.substring(12, 16);
    }

    /**
     * Generates a backend RequestId formatted as {@code 8-4-4-8} hex groups.
     */
    static String requestId()
    {
        final byte[] bytes = randomBytes(12);
        final String hex = hex(bytes);
        return hex.substring(0, 8) + "-"
            + hex.substring(8, 12) + "-"
            + hex.substring(12, 16) + "-"
            + hex.substring(16, 24);
    }

    static byte[] randomBytes(int count)
    {
        final byte[] bytes = new byte[count];
        RANDOM.nextBytes(bytes);
        return bytes;
    }

    static String hex(byte[] bytes)
    {
        final char[] out = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++)
        {
            final int value = bytes[i] & 0xff;
            out[i * 2] = HEX[value >>> 4];
            out[i * 2 + 1] = HEX[value & 0x0f];
        }
        return new String(out);
    }
}

