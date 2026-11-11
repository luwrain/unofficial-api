package org.luwrain.io.api.ya360.protocol;

import org.luwrain.io.api.ya360.Ya360ProtocolException;

/**
 * Resolves chat references to concrete chat ids.
 */
public final class ChatResolver
{
    private ChatResolver()
    {
    }

    /**
     * @return true for a literal chat id or thread id
     */
    public static boolean isChatId(String value)
    {
        return ThreadIds.isPrivateChatId(value)
            || ThreadIds.isNumericChatId(value)
            || ThreadIds.isThreadId(value);
    }

    /**
     * Builds a private chat id from a partner guid using locale independent UTF-16
     * code unit ordering.
     */
    public static String buildPrivateChatId(String partnerGuid, String myGuid)
    {
        if (partnerGuid == null || partnerGuid.isBlank())
        {
            throw new Ya360ProtocolException("partnerGuid is required");
        }
        if (myGuid == null || myGuid.isBlank())
        {
            throw new Ya360ProtocolException("myGuid is required");
        }
        final int cmp = partnerGuid.compareTo(myGuid);
        return cmp < 0 ? partnerGuid + "_" + myGuid : myGuid + "_" + partnerGuid;
    }

    /**
     * Resolves a reference to a chat id.
     *
     * <p>A literal chat/thread id is returned unchanged. A bare guid is treated as a
     * partner guid and resolved to a private chat id.</p>
     *
     * @throws Ya360ProtocolException when the reference is null or blank
     */
    public static String resolve(String ref, String myGuid)
    {
        if (ref == null || ref.isBlank())
        {
            throw new Ya360ProtocolException("chat reference is required");
        }
        if (isChatId(ref))
        {
            return ref;
        }
        return buildPrivateChatId(ref, myGuid);
    }
}

