package com.mpin.app.data;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

/**
 * Email settings (including the Gmail app password) are kept encrypted;
 * harmless UI preferences live in normal SharedPreferences.
 */
public final class SettingsStore {

    /** Auto-lock delay choices, in milliseconds. 0 = lock as soon as the app is left. */
    public static final long[] LOCK_DELAYS = {0, 30_000, 60_000, 5 * 60_000};

    private final File emailFile;
    private final SharedPreferences prefs;

    public SettingsStore(Context context) {
        Context app = context.getApplicationContext();
        emailFile = new File(app.getFilesDir(), "email.bin");
        prefs = app.getSharedPreferences("prefs", Context.MODE_PRIVATE);
    }

    public EmailSettings loadEmail() {
        EmailSettings s = new EmailSettings();
        String json = CryptoBox.read(emailFile);
        if (json == null) {
            return s;
        }
        try {
            JSONObject o = new JSONObject(json);
            s.enabled = o.optBoolean("enabled");
            s.senderEmail = o.optString("sender");
            s.appPassword = o.optString("password");
            s.recipientEmail = o.optString("recipient");
            s.includeMpin = o.optBoolean("includeMpin", true);
        } catch (JSONException ignored) {
            // corrupt file: fall back to defaults
        }
        return s;
    }

    public void saveEmail(EmailSettings s) throws IOException {
        try {
            JSONObject o = new JSONObject()
                    .put("enabled", s.enabled)
                    .put("sender", s.senderEmail)
                    .put("password", s.appPassword)
                    .put("recipient", s.recipientEmail)
                    .put("includeMpin", s.includeMpin);
            CryptoBox.write(emailFile, o.toString());
        } catch (JSONException e) {
            throw new IOException(e);
        }
    }

    public long lockDelayMs() {
        return prefs.getLong("lockDelay", 0);
    }

    public void setLockDelayMs(long delay) {
        prefs.edit().putLong("lockDelay", delay).apply();
    }

    public boolean isSetupBannerDismissed() {
        return prefs.getBoolean("bannerDismissed", false);
    }

    public void dismissSetupBanner() {
        prefs.edit().putBoolean("bannerDismissed", true).apply();
    }
}
