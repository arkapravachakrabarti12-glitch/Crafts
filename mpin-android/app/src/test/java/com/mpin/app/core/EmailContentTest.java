package com.mpin.app.core;

import org.junit.Test;

import java.time.ZoneId;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EmailContentTest {

    private final MpinRecord record = new MpinRecord("id", "048213", 6, "Bank <app> & UPI", 1_790_000_000_000L,
            MpinRecord.EmailStatus.PENDING);

    @Test
    public void includesAllDetails() {
        EmailContent c = EmailContent.forRecord(record, true, "Pixel 8", ZoneId.of("Asia/Kolkata"));
        assertEquals("New MPIN generated: Bank <app> & UPI", c.subject);
        assertTrue(c.text.contains("MPIN: 048213"));
        assertTrue(c.text.contains("Device: Pixel 8"));
        assertTrue(c.html.contains("Bank &lt;app&gt; &amp; UPI"));
        assertFalse(c.html.contains("<app>"));
        assertTrue(c.html.contains("048213"));
    }

    @Test
    public void masksMpinWhenAsked() {
        EmailContent c = EmailContent.forRecord(record, false, "Pixel 8", ZoneId.of("UTC"));
        assertFalse(c.text.contains("048213"));
        assertTrue(c.text.contains("••••13"));
    }
}
