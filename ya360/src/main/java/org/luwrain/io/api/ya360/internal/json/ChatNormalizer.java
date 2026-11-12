package org.luwrain.io.api.ya360.internal.json;

import com.google.gson.JsonObject;
import org.luwrain.io.api.ya360.internal.timestamps.Timestamps;
import org.luwrain.io.api.ya360.model.Chat;
import org.luwrain.io.api.ya360.model.Message;

import java.util.List;

/**
 * Normalizes a history response chat element into a {@link Chat} value object.
 */
public final class ChatNormalizer
{
    private ChatNormalizer()
    {
    }

    public static Chat normalize(JsonObject chatJson)
    {
        if (chatJson == null)
        {
            return null;
        }
        final String chatId = JsonExt.string(chatJson, "ChatId");
        final boolean privateChat = JsonExt.object(chatJson, "PrivateChatInfo") != null;
        final String kind = privateChat ? "private" : "group";

        String name = null;
        if (privateChat)
        {
            final JsonObject partner = JsonExt.object(chatJson, "PartnerInfo");
            if (partner != null)
            {
                name = JsonExt.string(partner, "DisplayName");
                if (name == null)
                {
                    name = JsonExt.string(partner, "PublicName");
                }
            }
        }
        else
        {
            final JsonObject chatInfo = JsonExt.object(chatJson, "ChatInfo");
            if (chatInfo != null)
            {
                name = JsonExt.string(chatInfo, "Name");
            }
        }

        final Long lastActivityMcs = JsonExt.longValue(chatJson, "LastTsMcs");
        final String lastActivityIso = lastActivityMcs == null ? null : Timestamps.microsToIso(lastActivityMcs);

        final Integer lastSeqNo = JsonExt.integer(chatJson, "LastSeqNo");
        final Integer lastSeenByMe = JsonExt.integer(chatJson, "LastSeenByMeSeqNo");
        final int unreadCount = lastSeqNo == null || lastSeenByMe == null
            ? 0
            : Math.max(0, lastSeqNo - lastSeenByMe);

        final boolean muted = Boolean.TRUE.equals(JsonExt.bool(chatJson, "Muted"));

        Message lastMessage = null;
        final List<JsonObject> messages = JsonExt.objectList(chatJson, "Messages");
        if (!messages.isEmpty())
        {
            final JsonObject first = messages.get(0);
            final JsonObject serverMessage = JsonExt.firstObject(first, "ServerMessage");
            if (serverMessage != null)
            {
                lastMessage = MessageNormalizer.normalize(serverMessage);
            }
            else if (first.has("ServerMessage"))
            {
                // A bare message object may itself be the ServerMessage wrapper.
                lastMessage = MessageNormalizer.normalize(first);
            }
        }

        final String lastActivityMcsString = lastActivityMcs == null ? null : Long.toString(lastActivityMcs);

        return new Chat(
            chatId,
            name,
            kind,
            lastActivityIso,
            lastActivityMcsString,
            unreadCount,
            unreadCount > 0,
            muted,
            lastMessage
        );
    }
}

