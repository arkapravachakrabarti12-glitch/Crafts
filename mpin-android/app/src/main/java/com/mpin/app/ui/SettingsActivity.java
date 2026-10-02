package com.mpin.app.ui;

import android.os.Bundle;
import android.util.Patterns;

import com.google.android.material.snackbar.Snackbar;
import com.mpin.app.R;
import com.mpin.app.core.EmailContent;
import com.mpin.app.data.EmailSettings;
import com.mpin.app.data.SettingsStore;
import com.mpin.app.databinding.ActivitySettingsBinding;
import com.mpin.app.mail.EmailWorker;
import com.mpin.app.mail.GmailSender;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SettingsActivity extends SecureActivity {

    private ActivitySettingsBinding b;
    private SettingsStore store;
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        applySystemBarInsets(b.getRoot());
        b.toolbar.setNavigationOnClickListener(v -> finish());

        store = new SettingsStore(this);
        EmailSettings s = store.loadEmail();
        b.emailEnabled.setChecked(s.enabled);
        b.sender.setText(s.senderEmail);
        b.password.setText(s.appPassword);
        b.recipient.setText(s.recipientEmail);
        b.includeMpin.setChecked(s.includeMpin);

        String[] delayNames = getResources().getStringArray(R.array.lock_delay_names);
        b.autoLock.setSimpleItems(delayNames);
        long current = store.lockDelayMs();
        for (int i = 0; i < SettingsStore.LOCK_DELAYS.length; i++) {
            if (SettingsStore.LOCK_DELAYS[i] == current) {
                b.autoLock.setText(delayNames[i], false);
            }
        }
        b.autoLock.setOnItemClickListener((parent, view, position, id) ->
                store.setLockDelayMs(SettingsStore.LOCK_DELAYS[position]));

        b.saveButton.setOnClickListener(v -> {
            EmailSettings form = readForm();
            if (form != null && save(form)) {
                Snackbar.make(b.getRoot(), R.string.settings_saved, Snackbar.LENGTH_SHORT).show();
            }
        });
        b.testButton.setOnClickListener(v -> sendTest());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        io.shutdown();
    }

    /** Validates the form; returns null (and shows errors) if something is wrong. */
    private EmailSettings readForm() {
        EmailSettings s = new EmailSettings();
        s.enabled = b.emailEnabled.isChecked();
        s.senderEmail = text(b.sender);
        s.appPassword = text(b.password).replace(" ", "");
        s.recipientEmail = text(b.recipient);
        s.includeMpin = b.includeMpin.isChecked();

        b.senderLayout.setError(null);
        b.passwordLayout.setError(null);
        b.recipientLayout.setError(null);

        boolean needsAll = s.enabled;
        boolean ok = true;
        if ((needsAll || !s.senderEmail.isEmpty()) && !Patterns.EMAIL_ADDRESS.matcher(s.senderEmail).matches()) {
            b.senderLayout.setError(getString(R.string.settings_invalid_email));
            ok = false;
        }
        if (needsAll && s.appPassword.isEmpty()) {
            b.passwordLayout.setError(getString(R.string.settings_password_required));
            ok = false;
        }
        if (!s.recipientEmail.isEmpty() && !Patterns.EMAIL_ADDRESS.matcher(s.recipientEmail).matches()) {
            b.recipientLayout.setError(getString(R.string.settings_invalid_email));
            ok = false;
        }
        return ok ? s : null;
    }

    private boolean save(EmailSettings s) {
        try {
            store.saveEmail(s);
            return true;
        } catch (IOException e) {
            Snackbar.make(b.getRoot(), R.string.settings_save_failed, Snackbar.LENGTH_LONG).show();
            return false;
        }
    }

    private void sendTest() {
        EmailSettings form = readForm();
        if (form == null) {
            return;
        }
        if (!form.isComplete()) {
            if (!Patterns.EMAIL_ADDRESS.matcher(form.senderEmail).matches()) {
                b.senderLayout.setError(getString(R.string.settings_invalid_email));
            }
            if (form.appPassword.isEmpty()) {
                b.passwordLayout.setError(getString(R.string.settings_password_required));
            }
            return;
        }
        if (!save(form)) {
            return;
        }
        b.testButton.setEnabled(false);
        Snackbar.make(b.getRoot(), R.string.settings_sending, Snackbar.LENGTH_INDEFINITE).show();
        io.execute(() -> {
            String error = null;
            try {
                GmailSender.send(form, EmailContent.test(EmailWorker.deviceName()));
            } catch (javax.mail.MessagingException e) {
                error = GmailSender.describe(e);
            } catch (RuntimeException e) {
                error = e.getMessage() != null ? e.getMessage() : e.toString();
            }
            String result = error == null ? getString(R.string.settings_test_ok, form.recipient()) : error;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                b.testButton.setEnabled(true);
                Snackbar.make(b.getRoot(), result, Snackbar.LENGTH_LONG).show();
            });
        });
    }

    private static String text(com.google.android.material.textfield.TextInputEditText field) {
        return field.getText() == null ? "" : field.getText().toString().trim();
    }
}
