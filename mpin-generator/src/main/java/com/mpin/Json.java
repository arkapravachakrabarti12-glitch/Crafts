package com.mpin;

import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/** Tiny JSON helpers for the few small payloads this app sends. */
final class Json {

    static final DateTimeFormatter TIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private Json() {
    }

    static String record(MpinRecord r) {
        return "{\"mpin\":\"" + r.mpin() + "\",\"length\":" + r.length()
                + ",\"label\":" + string(r.label())
                + ",\"generatedAt\":\"" + TIME.format(r.generatedAt()) + "\"}";
    }

    static String records(List<MpinRecord> records) {
        return records.stream().map(Json::record).collect(Collectors.joining(",", "[", "]"));
    }

    static String error(String message) {
        return "{\"error\":" + string(message) + "}";
    }

    /** Quoted, escaped JSON string (labels are user-typed, so escape everything). */
    static String string(String value) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '<' -> sb.append("\\u003c");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.append('"').toString();
    }

    static void send(HttpServletResponse resp, int status, String body) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Cache-Control", "no-store");
        resp.getWriter().write(body);
    }
}
