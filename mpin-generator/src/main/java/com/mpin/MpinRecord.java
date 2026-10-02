package com.mpin;

import java.time.LocalDateTime;

public record MpinRecord(String mpin, int length, String label, LocalDateTime generatedAt, String generatedBy) {
}
