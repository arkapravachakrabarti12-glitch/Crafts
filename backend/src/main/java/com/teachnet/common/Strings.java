package com.teachnet.common;

public final class Strings {

    private Strings() {}

    /** Trims the value and turns blank strings into null, so optional filters can be skipped. */
    public static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
