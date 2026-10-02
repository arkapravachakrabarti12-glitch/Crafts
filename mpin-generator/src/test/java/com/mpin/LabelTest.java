package com.mpin;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LabelTest {

    @Test
    void cleanLabelTrimsAndCollapsesWhitespace() {
        assertEquals("SBI mobile banking", GenerateMpinServlet.cleanLabel("  SBI \t mobile\n\nbanking  "));
        assertEquals("", GenerateMpinServlet.cleanLabel(null));
        assertEquals("", GenerateMpinServlet.cleanLabel(" \r\n "));
    }

    @Test
    void jsonEscapesUserTypedLabels() {
        assertEquals("\"a\\\"b\\\\c\\u003c/script>\\u0001\"", Json.string("a\"b\\c</script>\u0001"));
        assertEquals("\"Café ₹ बैंक\"", Json.string("Café ₹ बैंक"));
    }

    @Test
    void recordJsonIncludesLabel() {
        MpinRecord r = new MpinRecord("0471", 4, "UPI \"GPay\"", LocalDateTime.of(2026, 10, 2, 9, 30), "admin");
        assertEquals("{\"mpin\":\"0471\",\"length\":4,\"label\":\"UPI \\\"GPay\\\"\",\"generatedAt\":\"2026-10-02T09:30:00\"}",
                Json.record(r));
    }
}
