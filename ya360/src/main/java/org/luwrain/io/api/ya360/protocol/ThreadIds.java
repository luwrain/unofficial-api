package org.luwrain.io.api.ya360.protocol;

import org.luwrain.io.api.ya360.Ya360ProtocolException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pure string math for thread ids.
 */
public final class ThreadIds
{
    private static final Pattern PRIVATE_CHAT = Pattern.compile("(?i)^[0-9a-f-]{36}_[0-9a-f-]{36}$");
    private static final Pattern NUMERIC_CHAT = Pattern.compile("^(\\d+)/(\\d+)/(.+)$");
    private static final Pattern THREAD = Pattern.compile("^1(\\d\\d)/(\\d+)/(.+)_(\\d{16})$");

    private ThreadIds()
    {
    }

    /**
     * @return true when the value looks like a private two-guid chat id
     */
    public static boolean isPrivateChatId(String value)
    {
        return value != null && PRIVATE_CHAT.matcher(value).matches();
    }

    /**
     * @return true when the value looks like a group or channel chat id
     */
    public static boolean isNumericChatId(String value)
    {
        return value != null && NUMERIC_CHAT.matcher(value).matches();
    }

    /**
     * @return true when the value looks like a thread id
     */
    public static boolean isThreadId(String value)
    {
        return value != null && THREAD.matcher(value).matches();
    }

    /**
     * Builds a thread id from a parent chat id and a 16 digit microsecond timestamp string.
     *
     * @throws Ya360ProtocolException for unsupported chat prefixes
     */
    public static String buildThreadId(String chatId, String timestampMcs)
    {
        if (chatId == null || timestampMcs == null)
        {
            throw new Ya360ProtocolException("chatId and timestampMcs are required");
        }
        if (!timestampMcs.matches("\\d{16}"))
        {
            throw new Ya360ProtocolException("thread timestamp must be a 16 digit micros string");
        }

        final Matcher privateMatcher = PRIVATE_CHAT.matcher(chatId);
        if (privateMatcher.matches())
        {
            return "110/0/" + chatId + "_" + timestampMcs;
        }

        final Matcher numeric = NUMERIC_CHAT.matcher(chatId);
        if (numeric.matches())
        {
            final int prefix;
            try
            {
                prefix = Integer.parseInt(numeric.group(1));
            }
            catch (NumberFormatException e)
            {
                throw new Ya360ProtocolException("Unsupported chat prefix: " + numeric.group(1));
            }
            if (prefix == 0 || prefix == 1)
            {
                return String.valueOf(100 + prefix) + "/" + numeric.group(2) + "/" + numeric.group(3) + "_" + timestampMcs;
            }
            throw new Ya360ProtocolException("Unsupported chat prefix for thread derivation: " + prefix);
        }

        throw new Ya360ProtocolException("Unsupported chat id format for thread derivation: " + chatId);
    }

    /**
     * Parses a thread id back to its parent chat id.
     *
     * @return parent chat id, or null when the value is not a thread id
     */
    public static String parentChatId(String threadId)
    {
        final Matcher m = THREAD.matcher(threadId == null ? "" : threadId);
        if (!m.matches())
        {
            return null;
        }
        // Group 1 is the two zero-padded digits after the leading "1". It equals the
        // derivation prefix: 0 for group, 1 for channel, 10 for private.
        final int prefix = Integer.parseInt(m.group(1));
        final String ns = m.group(2);
        final String rest = m.group(3);
        if (prefix == 10)
        {
            return rest;
        }
        if (prefix == 0 || prefix == 1)
        {
            return prefix + "/" + ns + "/" + rest;
        }
        return null;
    }
}

