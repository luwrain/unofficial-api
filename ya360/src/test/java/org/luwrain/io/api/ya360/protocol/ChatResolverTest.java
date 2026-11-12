package org.luwrain.io.api.ya360.protocol;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChatResolverTest
{
    private static final String MY = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
    private static final String PARTNER_LESS = "00000000-0000-0000-0000-000000000000";
    private static final String PARTNER_MORE = "ffffffff-ffff-ffff-ffff-ffffffffffff";

    @Test
    void ordersPartnerBeforeMe()
    {
        assertEquals(PARTNER_LESS + "_" + MY, ChatResolver.buildPrivateChatId(PARTNER_LESS, MY));
    }

    @Test
    void ordersMeBeforePartner()
    {
        assertEquals(MY + "_" + PARTNER_MORE, ChatResolver.buildPrivateChatId(PARTNER_MORE, MY));
    }

    @Test
    void resolvesBareGuidToPrivateChat()
    {
        assertEquals(
            PARTNER_LESS + "_" + MY,
            ChatResolver.resolve(PARTNER_LESS, MY)
        );
    }

    @Test
    void resolvesLiteralThreadIdUnchanged()
    {
        assertEquals("100/1/abc_1234567890123456", ChatResolver.resolve("100/1/abc_1234567890123456", MY));
    }
}

