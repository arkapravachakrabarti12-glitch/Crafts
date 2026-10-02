package com.mpin.app.core;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class LabelsTest {

    @Test
    public void cleanTrimsAndCollapsesWhitespace() {
        assertEquals("SBI mobile banking", Labels.clean("  SBI \t mobile\n\nbanking  "));
        assertEquals("", Labels.clean(null));
        assertEquals("", Labels.clean(" \r\n "));
    }
}
