package org.luwrain.io.api.ya360;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class Ya360ConfigTest
{
    @Test
    void parsesCookieAndYandexUid() throws Exception
    {
        final Ya360Config config = new Ya360Config(
            "Session_id=abc; sessionid2=def; yandexuid=12345; yp=1",
            "42",
            "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"
        );
        assertEquals("12345", config.yandexUid());
        assertEquals("abc", config.cookieValue("Session_id"));
        assertEquals("42", config.myUid());
    }

    @Test
    void rejectsMissingSessionId()
    {
        assertThrows(Ya360ConfigException.class, () -> new Ya360Config(
            "yandexuid=12345",
            "42",
            "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"
        ));
    }

    @Test
    void rejectsMissingYandexUid()
    {
        assertThrows(Ya360ConfigException.class, () -> new Ya360Config(
            "Session_id=abc",
            "42",
            "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"
        ));
    }
}

