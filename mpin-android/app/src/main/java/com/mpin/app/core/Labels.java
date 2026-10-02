package com.mpin.app.core;

/** The "Label / Purpose" every MPIN is tagged with. */
public final class Labels {

    public static final int MAX_LENGTH = 60;

    private Labels() {
    }

    /** Trims, drops control characters and collapses runs of whitespace. */
    public static String clean(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replaceAll("\\p{Cntrl}", " ").replaceAll("\\s+", " ").trim();
    }
}
