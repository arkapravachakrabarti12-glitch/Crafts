package com.mpin.app.core;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MpinGeneratorTest {

    @Test
    public void generatesOnlyDigitsOfRequestedLength() {
        for (int length : new int[]{4, 6}) {
            Set<String> seen = new HashSet<>();
            for (int i = 0; i < 2000; i++) {
                String pin = MpinGenerator.generate(length);
                assertEquals(length, pin.length());
                assertTrue(pin, pin.matches("\\d+"));
                assertFalse(pin, MpinGenerator.isWeak(pin));
                seen.add(pin);
            }
            assertTrue("output should be well spread", seen.size() > 1000);
        }
    }

    @Test
    public void rejectsOtherLengths() {
        for (int length : new int[]{0, 3, 5, 7, 8, -4}) {
            try {
                MpinGenerator.generate(length);
                throw new AssertionError("accepted length " + length);
            } catch (IllegalArgumentException expected) {
                // ok
            }
        }
    }

    @Test
    public void flagsWeakPins() {
        for (String pin : new String[]{"0000", "1111", "1234", "4321", "7890", "0987", "1212", "1122", "2580",
                "000000", "123456", "654321", "890123", "121212", "123123", "112233", "111222"}) {
            assertTrue(pin, MpinGenerator.isWeak(pin));
        }
    }

    @Test
    public void acceptsOrdinaryPins() {
        assertFalse(MpinGenerator.isWeak("4829"));
        assertFalse(MpinGenerator.isWeak("0471"));
        assertFalse(MpinGenerator.isWeak("905317"));
    }
}
