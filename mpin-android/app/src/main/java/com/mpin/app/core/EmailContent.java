package com.mpin.app.core;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Subject and body of the "new MPIN generated" email. */
public final class EmailContent {

    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm:ss a z", Locale.ENGLISH);

    public final String subject;
    public final String text;
    public final String html;

    private EmailContent(String subject, String text, String html) {
        this.subject = subject;
        this.text = text;
        this.html = html;
    }

    public static EmailContent forRecord(MpinRecord r, boolean includeMpin, String deviceName, ZoneId zone) {
        String when = TIME.format(Instant.ofEpochMilli(r.createdAt).atZone(zone));
        String pin = includeMpin ? r.mpin : mask(r.mpin);

        String subject = "New MPIN generated: " + r.label;
        String text = "A new MPIN was generated in the MPIN Generator app.\n\n"
                + "Label / Purpose: " + r.label + "\n"
                + "MPIN: " + pin + "\n"
                + "Length: " + r.length + " digits\n"
                + "Generated at: " + when + "\n"
                + "Device: " + deviceName + "\n\n"
                + "If this wasn't you, open the app and review your MPINs.";

        String html = "<div style=\"font-family:Segoe UI,Roboto,Arial,sans-serif;background:#0b1020;padding:24px\">"
                + "<div style=\"max-width:480px;margin:auto;background:#141b33;border-radius:16px;padding:24px;color:#e8ecf8\">"
                + "<div style=\"font-size:13px;color:#8f9bbd;letter-spacing:.08em;text-transform:uppercase\">MPIN Generator</div>"
                + "<h2 style=\"margin:6px 0 18px;font-size:20px\">New MPIN generated</h2>"
                + "<div style=\"font-size:13px;color:#8f9bbd\">Label / Purpose</div>"
                + "<div style=\"font-size:17px;font-weight:600;margin-bottom:14px\">" + html(r.label) + "</div>"
                + "<div style=\"font-family:Consolas,monospace;font-size:34px;font-weight:700;letter-spacing:.3em;"
                + "color:#5eead4;background:#0b1020;border-radius:12px;padding:14px;text-align:center\">" + html(pin) + "</div>"
                + "<table style=\"width:100%;margin-top:16px;font-size:14px;color:#c9d1ea\">"
                + row("Length", r.length + " digits")
                + row("Generated at", when)
                + row("Device", deviceName)
                + "</table>"
                + "<p style=\"font-size:12px;color:#8f9bbd;margin-top:18px\">If this wasn't you, open the app and review your MPINs.</p>"
                + "</div></div>";

        return new EmailContent(subject, text, html);
    }

    public static EmailContent test(String deviceName) {
        String text = "Email alerts from MPIN Generator on " + deviceName + " are working.";
        return new EmailContent("MPIN Generator: test email", text,
                "<p style=\"font-family:Segoe UI,Roboto,Arial,sans-serif\">" + html(text) + "</p>");
    }

    static String mask(String pin) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < pin.length(); i++) {
            sb.append(i < pin.length() - 2 ? '•' : pin.charAt(i));
        }
        return sb.toString();
    }

    private static String row(String key, String value) {
        return "<tr><td style=\"color:#8f9bbd;padding:3px 0\">" + html(key) + "</td>"
                + "<td style=\"text-align:right;padding:3px 0\">" + html(value) + "</td></tr>";
    }

    static String html(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
