package com.mpin.app.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.mpin.app.core.MpinRecord;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** All generated MPINs (oldest first), kept in an encrypted file. Thread-safe. */
public final class MpinRepository {

    public static final int MAX_ENTRIES = 500;
    private static final String TAG = "MpinRepository";
    private static volatile MpinRepository instance;

    private final File file;
    private final List<MpinRecord> records = new ArrayList<>();
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();
    private final Handler main = new Handler(Looper.getMainLooper());

    private MpinRepository(Context context) {
        file = new File(context.getFilesDir(), "mpins.bin");
        load();
    }

    public static MpinRepository get(Context context) {
        if (instance == null) {
            synchronized (MpinRepository.class) {
                if (instance == null) {
                    instance = new MpinRepository(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    public synchronized List<MpinRecord> snapshot() {
        return new ArrayList<>(records);
    }

    public synchronized MpinRecord find(String id) {
        for (MpinRecord r : records) {
            if (r.id.equals(id)) {
                return r;
            }
        }
        return null;
    }

    public synchronized void add(MpinRecord record) {
        records.add(record);
        while (records.size() > MAX_ENTRIES) {
            records.remove(0);
        }
        saveAndNotify();
    }

    public synchronized void updateEmailStatus(String id, MpinRecord.EmailStatus status) {
        for (int i = 0; i < records.size(); i++) {
            if (records.get(i).id.equals(id)) {
                records.set(i, records.get(i).withEmailStatus(status));
                saveAndNotify();
                return;
            }
        }
    }

    public synchronized void clear() {
        records.clear();
        saveAndNotify();
    }

    /** Listeners are called on the main thread after every change. */
    public void addListener(Runnable listener) {
        listeners.add(listener);
    }

    public void removeListener(Runnable listener) {
        listeners.remove(listener);
    }

    private void load() {
        String json = CryptoBox.read(file);
        if (json == null) {
            return;
        }
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject o = array.getJSONObject(i);
                records.add(new MpinRecord(
                        o.getString("id"),
                        o.getString("mpin"),
                        o.getInt("length"),
                        o.getString("label"),
                        o.getLong("createdAt"),
                        MpinRecord.EmailStatus.valueOf(o.optString("email", "OFF"))));
            }
        } catch (JSONException | IllegalArgumentException e) {
            Log.w(TAG, "Could not read saved MPINs", e);
        }
    }

    private void saveAndNotify() {
        try {
            JSONArray array = new JSONArray();
            for (MpinRecord r : records) {
                array.put(new JSONObject()
                        .put("id", r.id)
                        .put("mpin", r.mpin)
                        .put("length", r.length)
                        .put("label", r.label)
                        .put("createdAt", r.createdAt)
                        .put("email", r.emailStatus.name()));
            }
            CryptoBox.write(file, array.toString());
        } catch (JSONException | IOException e) {
            Log.e(TAG, "Could not save MPINs", e);
        }
        main.post(() -> {
            for (Runnable l : listeners) {
                l.run();
            }
        });
    }
}
