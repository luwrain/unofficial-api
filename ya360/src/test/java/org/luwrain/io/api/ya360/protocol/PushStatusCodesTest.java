package org.luwrain.io.api.ya360.protocol;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PushStatusCodesTest
{
    @Test
    void recognizesSuccess()
    {
        assertTrue(PushStatusCodes.success(PushStatusCodes.FULLY_COMMITTED));
        assertTrue(PushStatusCodes.success(PushStatusCodes.DUPLICATE));
        assertFalse(PushStatusCodes.success(PushStatusCodes.FAILED));
    }

    @Test
    void recognizesDuplicate()
    {
        assertTrue(PushStatusCodes.duplicate(PushStatusCodes.DUPLICATE));
        assertFalse(PushStatusCodes.duplicate(PushStatusCodes.FULLY_COMMITTED));
    }

    @Test
    void names()
    {
        assertEquals("ENTITY_NOT_FOUND", AppErrorCodes.name(4));
        assertEquals("UNKNOWN(42)", AppErrorCodes.name(42));
    }
}

