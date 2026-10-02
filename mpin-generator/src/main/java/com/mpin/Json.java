package com.mpin;

import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/** Tiny JSON helpers — the payloads here are only digits, numbers and timestamps. */
final class Json {

    static final DateTimeFormatter TIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private Json() {
    }

    static String record(MpinRecord r) {
        return "{\"mpin\":\"" + r.mpin() + "\",\"length\":" + r.length()
                + ",\"generatedAt\":\"" + TIME.format(r.generatedAt()) + "\"}";
    }

    static String records(List<MpinRecord> records) {
        return records.stream().map(Json::record).collect(Collectors.joining(",", "[", "]"));
    }

    static String error(String message) {
        return "{\"error\":\"" + message.replace("\\", "\\\\").replace("\"", "\\\"") + "\"}";
    }

    static void send(HttpServletResponse resp, int status, String body) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Cache-Control", "no-store");
        resp.getWriter().write(body);
    }
}
