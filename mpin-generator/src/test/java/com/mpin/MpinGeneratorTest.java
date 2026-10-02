package com.mpin;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MpinGeneratorTest {

    @ParameterizedTest
    @ValueSource(ints = {4, 6})
    void generatesOnlyDigitsOfRequestedLength(int length) {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 2000; i++) {
            String pin = MpinGenerator.generate(length);
            assertEquals(length, pin.length());
            assertTrue(pin.matches("\\d+"), pin);
            assertFalse(MpinGenerator.isWeak(pin), pin);
            seen.add(pin);
        }
        assertTrue(seen.size() > 1000, "output should be well spread");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 3, 5, 7, 8, -4})
    void rejectsOtherLengths(int length) {
        assertThrows(IllegalArgumentException.class, () -> MpinGenerator.generate(length));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0000", "1111", "1234", "4321", "7890", "0987", "1212", "1122", "2580",
            "000000", "123456", "654321", "890123", "121212", "123123", "112233", "111222"})
    void flagsWeakPins(String pin) {
        assertTrue(MpinGenerator.isWeak(pin), pin);
    }

    @Test
    void acceptsOrdinaryPins() {
        assertFalse(MpinGenerator.isWeak("4829"));
        assertFalse(MpinGenerator.isWeak("0471"));
        assertFalse(MpinGenerator.isWeak("905317"));
    }
}
