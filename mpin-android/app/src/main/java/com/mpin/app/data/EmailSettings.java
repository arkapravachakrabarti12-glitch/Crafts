package com.mpin.app.data;

import android.util.Patterns;

/** Where and how "new MPIN" emails are sent. */
public final class EmailSettings {

    public boolean enabled;
    /** The Gmail account that sends the email. */
    public String senderEmail = "";
    /** A 16-character Google App Password (not the normal Gmail password). */
    public String appPassword = "";
    /** Who receives it; defaults to the sender. */
    public String recipientEmail = "";
    public boolean includeMpin = true;

    public boolean isComplete() {
        return Patterns.EMAIL_ADDRESS.matcher(senderEmail).matches()
                && !appPassword.isEmpty()
                && Patterns.EMAIL_ADDRESS.matcher(recipient()).matches();
    }

    public boolean isActive() {
        return enabled && isComplete();
    }

    public String recipient() {
        return recipientEmail.isEmpty() ? senderEmail : recipientEmail;
    }
}
