package com.mpin.app.core;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Generates cryptographically random 4- or 6-digit MPINs and rejects
 * the easy-to-guess ones (1111, 1234, 121212, 2580...).
 */
public final class MpinGenerator {

    /** Frequently used PINs that are not caught by the pattern checks. */
    private static final Set<String> COMMON = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "2580", "0852", "1004", "2000", "1010", "6969", "1122", "1313",
            "4321", "1212", "7777", "1379", "1397", "2468", "1357",
            "112233", "123321", "159753", "147258", "258369", "696969", "654321")));

    private static final SecureRandom RANDOM = new SecureRandom();

    private MpinGenerator() {
    }

    public static boolean isAllowedLength(int length) {
        return length == 4 || length == 6;
    }

    public static String generate(int length) {
        if (!isAllowedLength(length)) {
            throw new IllegalArgumentException("MPIN length must be 4 or 6");
        }
        while (true) {
            StringBuilder pin = new StringBuilder(length);
            for (int i = 0; i < length; i++) {
                pin.append(RANDOM.nextInt(10));
            }
            String candidate = pin.toString();
            if (!isWeak(candidate)) {
                return candidate;
            }
        }
    }

    /** True for PINs that are trivially guessable. */
    public static boolean isWeak(String pin) {
        return COMMON.contains(pin)
                || isSequential(pin)
                || repeatsShorterBlock(pin)
                || distinctDigits(pin) <= 2;   // 1122, 1211, 222333 ...
    }

    /** 1234, 4321, 7890, 3210 ... (step of +1 or -1, wrapping 9 -> 0). */
    private static boolean isSequential(String pin) {
        boolean up = true;
        boolean down = true;
        for (int i = 1; i < pin.length(); i++) {
            int prev = pin.charAt(i - 1) - '0';
            int cur = pin.charAt(i) - '0';
            up &= cur == (prev + 1) % 10;
            down &= cur == (prev + 9) % 10;
        }
        return up || down;
    }

    /** 1111, 1212, 121212, 123123 ... */
    private static boolean repeatsShorterBlock(String pin) {
        int n = pin.length();
        for (int block = 1; block <= n / 2; block++) {
            if (n % block != 0) {
                continue;
            }
            String unit = pin.substring(0, block);
            StringBuilder repeated = new StringBuilder(n);
            for (int i = 0; i < n / block; i++) {
                repeated.append(unit);
            }
            if (repeated.toString().equals(pin)) {
                return true;
            }
        }
        return false;
    }

    private static int distinctDigits(String pin) {
        return (int) pin.chars().distinct().count();
    }
}
