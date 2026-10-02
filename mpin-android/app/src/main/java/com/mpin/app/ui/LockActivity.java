package com.mpin.app.ui;

import android.app.KeyguardManager;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.WindowManager;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.SystemBarStyle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.mpin.app.R;
import com.mpin.app.databinding.ActivityLockBinding;
import com.mpin.app.security.AppLock;

/**
 * Lock screen. Unlocks with the phone's own security: fingerprint (or other strong
 * biometric) and, as an alternative, the device PIN, pattern or password.
 */
public class LockActivity extends AppCompatActivity {

    private ActivityLockBinding binding;
    private BiometricPrompt prompt;
    private boolean autoPrompted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this, SystemBarStyle.dark(Color.TRANSPARENT), SystemBarStyle.dark(Color.TRANSPARENT));
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        binding = ActivityLockBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        SecureActivity.applySystemBarInsets(binding.getRoot());

        prompt = new BiometricPrompt(this, ContextCompat.getMainExecutor(this), new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                onUnlocked();
            }

            @Override
            public void onAuthenticationError(int code, @NonNull CharSequence message) {
                boolean cancelled = code == BiometricPrompt.ERROR_USER_CANCELED
                        || code == BiometricPrompt.ERROR_NEGATIVE_BUTTON
                        || code == BiometricPrompt.ERROR_CANCELED;
                binding.status.setText(cancelled ? getString(R.string.lock_tap_to_unlock) : message);
            }
        });

        binding.unlockButton.setOnClickListener(v -> authenticate());
        binding.setupButton.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_SECURITY_SETTINGS)));

        // Back from the lock screen leaves the app instead of revealing what's behind it.
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                moveTaskToBack(true);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!AppLock.get(this).isLocked()) {
            onUnlocked();
            return;
        }
        if (!isDeviceSecure()) {
            showSetupNeeded(true);
            return;
        }
        showSetupNeeded(false);
        if (!autoPrompted) {
            autoPrompted = true;
            authenticate();
        }
    }

    private void authenticate() {
        if (!isDeviceSecure()) {
            showSetupNeeded(true);
            return;
        }
        // Android 9-10 only allow "biometric OR device credential" with the weak class.
        int authenticators = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                ? BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.DEVICE_CREDENTIAL
                : BiometricManager.Authenticators.BIOMETRIC_WEAK | BiometricManager.Authenticators.DEVICE_CREDENTIAL;

        BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.lock_prompt_title))
                .setSubtitle(getString(R.string.lock_prompt_subtitle))
                .setAllowedAuthenticators(authenticators)
                .build();
        binding.status.setText("");
        prompt.authenticate(info);
    }

    private boolean isDeviceSecure() {
        KeyguardManager km = ContextCompat.getSystemService(this, KeyguardManager.class);
        return km != null && km.isDeviceSecure();
    }

    private void showSetupNeeded(boolean needed) {
        binding.setupGroup.setVisibility(needed ? View.VISIBLE : View.GONE);
        binding.unlockButton.setVisibility(needed ? View.GONE : View.VISIBLE);
        binding.subtitle.setText(needed ? R.string.lock_setup_needed : R.string.lock_subtitle);
    }

    private void onUnlocked() {
        AppLock.get(this).unlock();
        if (isTaskRoot()) {
            startActivity(new Intent(this, MainActivity.class));
        }
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}
