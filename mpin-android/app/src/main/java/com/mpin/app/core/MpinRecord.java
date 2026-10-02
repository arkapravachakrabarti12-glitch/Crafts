package com.mpin.app.core;

/** One generated MPIN. Immutable; use {@link #withEmailStatus} to change the status. */
public final class MpinRecord {

    public enum EmailStatus { OFF, PENDING, SENT, FAILED }

    public final String id;
    public final String mpin;
    public final int length;
    public final String label;
    public final long createdAt;
    public final EmailStatus emailStatus;

    public MpinRecord(String id, String mpin, int length, String label, long createdAt, EmailStatus emailStatus) {
        this.id = id;
        this.mpin = mpin;
        this.length = length;
        this.label = label;
        this.createdAt = createdAt;
        this.emailStatus = emailStatus;
    }

    public MpinRecord withEmailStatus(EmailStatus status) {
        return new MpinRecord(id, mpin, length, label, createdAt, status);
    }
}
