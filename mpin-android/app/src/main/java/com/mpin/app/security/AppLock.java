package com.mpin.app.security;

import android.content.Context;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import com.mpin.app.data.SettingsStore;

/**
 * App-wide lock state. The app starts locked and locks again when it has been in
 * the background longer than the auto-lock delay chosen in Settings.
 */
public final class AppLock implements DefaultLifecycleObserver {

    private static volatile AppLock instance;

    private final SettingsStore settings;
    private volatile boolean locked = true;
    private long backgroundedAt;
    private boolean ignoreNextBackground;

    private AppLock(Context context) {
        settings = new SettingsStore(context);
    }

    public static AppLock get(Context context) {
        if (instance == null) {
            synchronized (AppLock.class) {
                if (instance == null) {
                    instance = new AppLock(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    public boolean isLocked() {
        return locked;
    }

    public void unlock() {
        locked = false;
    }

    public void lock() {
        locked = true;
    }

    /**
     * Call right before opening a system screen we expect to come back from
     * (e.g. the "Save as" file picker), so returning from it doesn't lock the app.
     */
    public void ignoreNextBackground() {
        ignoreNextBackground = true;
    }

    /** Called when an activity resumes; drops a stale ignore flag that was never used. */
    public void clearIgnoreFlag() {
        ignoreNextBackground = false;
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        // Whole app went to the background (home button, screen off, another app).
        if (!locked && !ignoreNextBackground) {
            backgroundedAt = SystemClock.elapsedRealtime();
        }
        ignoreNextBackground = false;
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        if (backgroundedAt > 0) {
            long away = SystemClock.elapsedRealtime() - backgroundedAt;
            if (away >= settings.lockDelayMs()) {
                locked = true;
            }
            backgroundedAt = 0;
        }
    }
}
