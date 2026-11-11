package org.luwrain.io.api.ya360.internal.json;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.luwrain.io.api.ya360.model.Message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessageNormalizerTest
{
    @Test
    void normalizesTextMessage()
    {
        final Message message = MessageNormalizer.normalize(JsonParser.parseString("""
            {
              "ClientMessage": {
                "Plain": {
                  "ChatId": "chat-1",
                  "PayloadId": "payload-1",
                  "Text": { "MessageText": "hello" }
                }
              },
              "ServerMessageInfo": {
                "Timestamp": 1700000000123456,
                "SeqNo": 5,
                "From": { "Guid": "guid-1", "DisplayName": "Alice" },
                "Deleted": false,
                "Version": 1,
                "PrevTimestamp": 1700000000000000,
                "LastEditTimestamp": 0
              }
            }
            """).getAsJsonObject());

        assertNotNull(message);
        assertEquals("text", message.kind());
        assertEquals("hello", message.text());
        assertEquals("1700000000123456", message.id());
        assertEquals("1700000000123456", message.timestampMcs());
        assertEquals(5, message.seqNo());
        assertEquals("guid-1", message.from().guid());
        assertFalse(message.deleted());
        assertFalse(message.edited());
    }

    @Test
    void normalizesDeletedAndEdited()
    {
        final Message message = MessageNormalizer.normalize(JsonParser.parseString("""
            {
              "ClientMessage": {
                "Plain": {
                  "ChatId": "chat-1",
                  "Text": { "MessageText": "old" }
                }
              },
              "ServerMessageInfo": {
                "Timestamp": 1700000000123456,
                "Deleted": true,
                "LastEditTimestamp": 1700000000999999
              }
            }
            """).getAsJsonObject());

        assertEquals("deleted", message.kind());
        assertTrue(message.deleted());
        assertTrue(message.edited());
    }

    @Test
    void normalizesImageAttachment()
    {
        final Message message = MessageNormalizer.normalize(JsonParser.parseString("""
            {
              "ClientMessage": {
                "Plain": {
                  "ChatId": "chat-1",
                  "Image": {
                    "FileInfo": {
                      "Id2": "bucket/uuid",
                      "Name": "pic.png",
                      "Size": 123,
                      "Source": 0
                    },
                    "Width": 100,
                    "Height": 200
                  }
                }
              },
              "ServerMessageInfo": { "Timestamp": 1700000000123456 }
            }
            """).getAsJsonObject());

        assertEquals("image", message.kind());
        assertNull(message.text());
        assertEquals(1, message.attachments().size());
        assertEquals("bucket/uuid", message.attachments().get(0).fileId());
        assertEquals("mds", message.attachments().get(0).source());
        assertEquals(100, message.attachments().get(0).width());
    }

    @Test
    void normalizesVoiceTextAndDiskSource()
    {
        final Message message = MessageNormalizer.normalize(JsonParser.parseString("""
            {
              "ClientMessage": {
                "Plain": {
                  "ChatId": "chat-1",
                  "Voice": {
                    "Text": { "MessageText": "recognized" },
                    "FileInfo": { "Id2": "disk/uuid", "Source": 1 }
                  }
                }
              },
              "ServerMessageInfo": { "Timestamp": 1700000000123456 }
            }
            """).getAsJsonObject());

        assertEquals("voice", message.kind());
        assertEquals("recognized", message.text());
        assertEquals("disk", message.attachments().get(0).source());
    }

    @Test
    void unknownWhenNoContent()
    {
        final Message message = MessageNormalizer.normalize(JsonParser.parseString("""
            {
              "ClientMessage": {},
              "ServerMessageInfo": { "Timestamp": 1700000000123456 }
            }
            """).getAsJsonObject());

        assertEquals("unknown", message.kind());
    }
}

