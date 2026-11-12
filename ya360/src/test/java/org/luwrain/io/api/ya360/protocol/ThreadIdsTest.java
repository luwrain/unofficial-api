package org.luwrain.io.api.ya360.protocol;

import org.junit.jupiter.api.Test;
import org.luwrain.io.api.ya360.Ya360ProtocolException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThreadIdsTest
{
    private static final String MY = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
    private static final String OTHER = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";

    @Test
    void derivesPrivateThread()
    {
        final String chatId = ChatResolver.buildPrivateChatId(MY, OTHER);
        final String threadId = ThreadIds.buildThreadId(chatId, "1234567890123456");
        assertTrue(threadId.startsWith("110/0/"));
        assertEquals(chatId, ThreadIds.parentChatId(threadId));
    }

    @Test
    void derivesGroupThread()
    {
        final String threadId = ThreadIds.buildThreadId("0/123/abc", "1234567890123456");
        assertEquals("100/123/abc_1234567890123456", threadId);
        assertEquals("0/123/abc", ThreadIds.parentChatId(threadId));
    }

    @Test
    void derivesChannelThread()
    {
        final String threadId = ThreadIds.buildThreadId("1/7/abc", "1234567890123456");
        assertEquals("101/7/abc_1234567890123456", threadId);
        assertEquals("1/7/abc", ThreadIds.parentChatId(threadId));
    }

    @Test
    void rejectsUnsupportedPrefix()
    {
        assertThrows(Ya360ProtocolException.class, () -> ThreadIds.buildThreadId("2/7/abc", "1234567890123456"));
    }

    @Test
    void detectsThreadIds()
    {
        assertTrue(ThreadIds.isThreadId("100/123/abc_1234567890123456"));
        assertFalse(ThreadIds.isThreadId("0/123/abc"));
        assertFalse(ThreadIds.isThreadId("2/123/abc_1234567890123456"));
        assertNull(ThreadIds.parentChatId("not-a-thread"));
    }
}

