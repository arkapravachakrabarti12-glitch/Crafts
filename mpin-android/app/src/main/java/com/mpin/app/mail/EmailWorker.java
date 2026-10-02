package com.mpin.app.mail;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.mpin.app.core.EmailContent;
import com.mpin.app.core.MpinRecord;
import com.mpin.app.data.EmailSettings;
import com.mpin.app.data.MpinRepository;
import com.mpin.app.data.SettingsStore;

import java.time.ZoneId;
import java.util.concurrent.TimeUnit;

/**
 * Emails one generated MPIN. Runs when the phone is online and retries a few times
 * if the network fails. Only the record id is passed in; the MPIN itself is read
 * from encrypted storage, so it never sits in WorkManager's database.
 */
public class EmailWorker extends Worker {

    public static final String TAG = "mpin-email";
    private static final String KEY_ID = "id";
    private static final int MAX_ATTEMPTS = 5;

    public EmailWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    public static void enqueue(Context context, String recordId) {
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(EmailWorker.class)
                .setInputData(new Data.Builder().putString(KEY_ID, recordId).build())
                .setConstraints(new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .addTag(TAG)
                .build();
        WorkManager.getInstance(context).enqueueUniqueWork("email-" + recordId, ExistingWorkPolicy.KEEP, request);
    }

    public static String deviceName() {
        String model = Build.MODEL == null ? "" : Build.MODEL;
        String maker = Build.MANUFACTURER == null ? "" : Build.MANUFACTURER;
        if (model.toLowerCase().startsWith(maker.toLowerCase())) {
            return model;
        }
        return (maker.isEmpty() ? "" : Character.toUpperCase(maker.charAt(0)) + maker.substring(1) + " ") + model;
    }

    @NonNull
    @Override
    public Result doWork() {
        String id = getInputData().getString(KEY_ID);
        MpinRepository repo = MpinRepository.get(getApplicationContext());
        MpinRecord record = id == null ? null : repo.find(id);
        if (record == null) {
            return Result.success();   // history was cleared meanwhile
        }

        EmailSettings settings = new SettingsStore(getApplicationContext()).loadEmail();
        if (!settings.isActive()) {
            repo.updateEmailStatus(id, MpinRecord.EmailStatus.OFF);
            return Result.success();
        }

        try {
            GmailSender.send(settings, EmailContent.forRecord(record, settings.includeMpin, deviceName(), ZoneId.systemDefault()));
            repo.updateEmailStatus(id, MpinRecord.EmailStatus.SENT);
            return Result.success();
        } catch (javax.mail.AuthenticationFailedException e) {
            Log.w(TAG, "Gmail login failed", e);
            repo.updateEmailStatus(id, MpinRecord.EmailStatus.FAILED);
            return Result.failure();
        } catch (Exception e) {
            Log.w(TAG, "Email failed (attempt " + (getRunAttemptCount() + 1) + ")", e);
            if (getRunAttemptCount() + 1 < MAX_ATTEMPTS) {
                return Result.retry();
            }
            repo.updateEmailStatus(id, MpinRecord.EmailStatus.FAILED);
            return Result.failure();
        }
    }
}
