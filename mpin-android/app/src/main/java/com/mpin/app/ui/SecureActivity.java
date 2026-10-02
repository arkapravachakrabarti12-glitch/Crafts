package com.mpin.app.ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.mpin.app.security.AppLock;

/**
 * Base for every screen that shows MPINs: blocks screenshots / the recents preview,
 * and sends the user to the lock screen whenever the app is locked.
 */
public abstract class SecureActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this, SystemBarStyle.dark(Color.TRANSPARENT), SystemBarStyle.dark(Color.TRANSPARENT));
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
    }

    /** Pads {@code root} so content stays clear of the status bar, nav bar and keyboard. */
    protected static void applySystemBarInsets(View root) {
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Hide content before the first frame if we're about to show the lock screen.
        findViewById(android.R.id.content).setVisibility(
                AppLock.get(this).isLocked() ? View.INVISIBLE : View.VISIBLE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        AppLock lock = AppLock.get(this);
        lock.clearIgnoreFlag();
        if (lock.isLocked()) {
            startActivity(new Intent(this, LockActivity.class));
            overridePendingTransition(0, 0);
        } else {
            findViewById(android.R.id.content).setVisibility(View.VISIBLE);
        }
    }
}
