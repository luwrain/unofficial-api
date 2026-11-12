package org.luwrain.io.api.ya360.internal.json;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.luwrain.io.api.ya360.model.Chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatNormalizerTest
{
    @Test
    void normalizesPrivateChatAndClampsUnread()
    {
        final Chat chat = ChatNormalizer.normalize(JsonParser.parseString("""
            {
              "ChatId": "a_b",
              "PrivateChatInfo": {},
              "PartnerInfo": { "Guid": "b", "DisplayName": "Bob" },
              "LastSeqNo": 10,
              "LastSeenByMeSeqNo": 12,
              "LastTsMcs": 1700000000123456,
              "Muted": true,
              "Messages": []
            }
            """).getAsJsonObject());

        assertEquals("private", chat.kind());
        assertEquals("Bob", chat.name());
        assertEquals(0, chat.unreadCount());
        assertFalse(chat.unread());
        assertTrue(chat.muted());
        assertEquals("1700000000123456", chat.lastActivityMcs());
    }

    @Test
    void normalizesGroupChatAndUnread()
    {
        final Chat chat = ChatNormalizer.normalize(JsonParser.parseString("""
            {
              "ChatId": "0/0/g",
              "ChatInfo": { "Name": "Group" },
              "LastSeqNo": 20,
              "LastSeenByMeSeqNo": 4,
              "LastTsMcs": 1700000000123456,
              "Messages": [
                {
                  "ServerMessage": {
                    "ClientMessage": {
                      "Plain": {
                        "ChatId": "0/0/g",
                        "Text": { "MessageText": "hi" }
                      }
                    },
                    "ServerMessageInfo": {
                      "Timestamp": 1700000000123456,
                      "SeqNo": 20
                    }
                  }
                }
              ]
            }
            """).getAsJsonObject());

        assertEquals("group", chat.kind());
        assertEquals("Group", chat.name());
        assertEquals(16, chat.unreadCount());
        assertTrue(chat.unread());
        assertNotNull(chat.lastMessage());
        assertEquals("hi", chat.lastMessage().text());
    }
}

