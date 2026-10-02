package com.mpin;

import java.time.LocalDateTime;

public record MpinRecord(String mpin, int length, LocalDateTime generatedAt, String generatedBy) {
}
