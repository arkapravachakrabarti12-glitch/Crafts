package com.mpin.app;

import android.app.Application;

import androidx.lifecycle.ProcessLifecycleOwner;

import com.mpin.app.security.AppLock;

public class MpinApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        ProcessLifecycleOwner.get().getLifecycle().addObserver(AppLock.get(this));
    }
}
