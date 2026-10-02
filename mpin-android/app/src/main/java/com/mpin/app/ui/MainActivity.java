package com.mpin.app.ui;

import android.animation.ValueAnimator;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PersistableBundle;
import android.os.SystemClock;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.work.WorkManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.mpin.app.R;
import com.mpin.app.core.Labels;
import com.mpin.app.core.MpinGenerator;
import com.mpin.app.core.MpinRecord;
import com.mpin.app.core.XlsxWriter;
import com.mpin.app.data.EmailSettings;
import com.mpin.app.data.MpinRepository;
import com.mpin.app.data.SettingsStore;
import com.mpin.app.databinding.ActivityMainBinding;
import com.mpin.app.mail.EmailWorker;
import com.mpin.app.security.AppLock;

import java.io.OutputStream;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends SecureActivity {

    private static final String STATE_CURRENT = "current";
    private static final String STATE_HIDDEN = "hidden";
    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final long CLIPBOARD_CLEAR_MS = 45_000;

    private ActivityMainBinding b;
    private MpinRepository repo;
    private SettingsStore settings;
    private final HistoryAdapter adapter = new HistoryAdapter();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Runnable onHistoryChanged = this::renderHistory;

    private MpinRecord current;
    private boolean hidden;
    private boolean rolling;

    private final ActivityResultLauncher<String> exportLauncher =
            registerForActivityResult(new ActivityResultContracts.CreateDocument(XLSX), this::exportTo);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        applySystemBarInsets(b.getRoot());

        repo = MpinRepository.get(this);
        settings = new SettingsStore(this);

        b.toolbar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_lock) {
                AppLock.get(this).lock();
                startActivity(new Intent(this, LockActivity.class));
            } else if (id == R.id.action_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
            } else if (id == R.id.action_export) {
                startExport();
            } else if (id == R.id.action_clear) {
                confirmClear();
            } else {
                return false;
            }
            return true;
        });

        b.historyList.setLayoutManager(new LinearLayoutManager(this));
        b.historyList.setAdapter(adapter);

        b.generateButton.setOnClickListener(v -> generate());
        b.copyButton.setOnClickListener(v -> copy());
        b.visibilityButton.setOnClickListener(v -> setHidden(!hidden));
        b.exportButton.setOnClickListener(v -> startExport());
        b.lengthGroup.addOnButtonCheckedListener((group, id, checked) -> {
            int length = id == R.id.length6 ? 6 : 4;
            if (checked && !rolling && (current == null || current.length != length)) {
                current = null;
                renderEmptyDigits();
            }
        });
        b.labelInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO) {
                generate();
                return true;
            }
            return false;
        });
        b.labelInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().length() > 0) {
                    b.labelLayout.setError(null);
                }
            }
            @Override public void afterTextChanged(android.text.Editable s) { }
        });

        b.bannerSetup.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        b.bannerDismiss.setOnClickListener(v -> {
            settings.dismissSetupBanner();
            b.setupBanner.setVisibility(View.GONE);
        });

        if (savedInstanceState != null) {
            hidden = savedInstanceState.getBoolean(STATE_HIDDEN);
            String id = savedInstanceState.getString(STATE_CURRENT);
            current = id == null ? null : repo.find(id);
            if (current != null) {
                b.lengthGroup.check(current.length == 6 ? R.id.length6 : R.id.length4);
            }
        }
        adapter.setHidden(hidden);
        updateVisibilityButton();
        if (current != null) {
            showFinal(current);
        } else {
            renderEmptyDigits();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        repo.addListener(onHistoryChanged);
        renderHistory();
        EmailSettings email = settings.loadEmail();
        b.setupBanner.setVisibility(!email.isComplete() && !settings.isSetupBannerDismissed() ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        repo.removeListener(onHistoryChanged);
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        // Only the id: the MPIN itself is never written to the saved-state bundle.
        out.putString(STATE_CURRENT, current == null ? null : current.id);
        out.putBoolean(STATE_HIDDEN, hidden);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        io.shutdown();
    }

    // ---------------------------------------------------------------- generate

    private int selectedLength() {
        return b.lengthGroup.getCheckedButtonId() == R.id.length6 ? 6 : 4;
    }

    private void generate() {
        if (rolling) {
            return;
        }
        String label = Labels.clean(b.labelInput.getText() == null ? "" : b.labelInput.getText().toString());
        if (label.isEmpty()) {
            b.labelLayout.setError(getString(R.string.label_required));
            b.labelInput.requestFocus();
            b.labelLayout.animate().translationX(12).setDuration(50)
                    .withEndAction(() -> b.labelLayout.animate().translationX(-12).setDuration(50)
                            .withEndAction(() -> b.labelLayout.animate().translationX(0).setDuration(50)));
            return;
        }
        b.labelLayout.setError(null);

        EmailSettings email = settings.loadEmail();
        int length = selectedLength();
        MpinRecord record = new MpinRecord(
                UUID.randomUUID().toString(),
                MpinGenerator.generate(length),
                length,
                label,
                System.currentTimeMillis(),
                email.isActive() ? MpinRecord.EmailStatus.PENDING : MpinRecord.EmailStatus.OFF);
        repo.add(record);
        if (email.isActive()) {
            EmailWorker.enqueue(this, record.id);
        } else if (email.enabled) {
            Snackbar.make(b.getRoot(), R.string.email_not_setup, Snackbar.LENGTH_LONG)
                    .setAction(R.string.settings, v -> startActivity(new Intent(this, SettingsActivity.class)))
                    .show();
        }
        rollTo(record);
    }

    // ---------------------------------------------------------------- digits

    private List<TextView> buildDigitBoxes(int length) {
        b.digitRow.removeAllViews();
        int width = dp(length == 6 ? 46 : 62);
        int gap = dp(length == 6 ? 5 : 8);
        List<TextView> boxes = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            TextView tv = new TextView(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(width, width * 4 / 3);
            lp.setMargins(gap / 2, 0, gap / 2, 0);
            tv.setLayoutParams(lp);
            tv.setGravity(Gravity.CENTER);
            tv.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, length == 6 ? 28 : 34);
            tv.setTextColor(ContextCompat.getColor(this, R.color.text));
            tv.setBackgroundResource(R.drawable.bg_digit);
            b.digitRow.addView(tv);
            boxes.add(tv);
        }
        return boxes;
    }

    private void renderEmptyDigits() {
        for (TextView tv : buildDigitBoxes(selectedLength())) {
            tv.setText("–");
            tv.setTextColor(ContextCompat.getColor(this, R.color.outline));
        }
        b.pinLabel.setVisibility(View.GONE);
        b.copyButton.setEnabled(false);
        b.visibilityButton.setEnabled(false);
    }

    private void showFinal(MpinRecord r) {
        List<TextView> boxes = buildDigitBoxes(r.length);
        for (int i = 0; i < boxes.size(); i++) {
            boxes.get(i).setText(hidden ? "•" : String.valueOf(r.mpin.charAt(i)));
            boxes.get(i).setBackgroundResource(R.drawable.bg_digit_settled);
        }
        b.pinLabel.setText(getString(R.string.for_label, r.label));
        b.pinLabel.setVisibility(View.VISIBLE);
        b.copyButton.setEnabled(true);
        b.visibilityButton.setEnabled(true);
    }

    /** Slot-machine style reveal: each digit spins, then settles left to right. */
    private void rollTo(MpinRecord r) {
        current = r;
        if (!ValueAnimator.areAnimatorsEnabled()) {
            showFinal(r);
            return;
        }
        rolling = true;
        b.generateButton.setEnabled(false);
        b.pinLabel.setVisibility(View.GONE);
        List<TextView> boxes = buildDigitBoxes(r.length);
        int muted = ContextCompat.getColor(this, R.color.muted);
        int text = ContextCompat.getColor(this, R.color.text);
        boolean[] settled = new boolean[boxes.size()];
        SecureRandom random = new SecureRandom();
        long start = SystemClock.uptimeMillis();

        Runnable tick = new Runnable() {
            @Override
            public void run() {
                long t = SystemClock.uptimeMillis() - start;
                boolean done = true;
                for (int i = 0; i < boxes.size(); i++) {
                    TextView tv = boxes.get(i);
                    if (t >= 380 + i * 110L) {
                        if (!settled[i]) {
                            settled[i] = true;
                            tv.setTextColor(text);
                            tv.setText(hidden ? "•" : String.valueOf(r.mpin.charAt(i)));
                            tv.setBackgroundResource(R.drawable.bg_digit_settled);
                            tv.setScaleX(0.9f);
                            tv.setScaleY(0.9f);
                            tv.animate().scaleX(1f).scaleY(1f).setDuration(220).start();
                        }
                    } else {
                        done = false;
                        tv.setTextColor(muted);
                        tv.setText(String.valueOf(random.nextInt(10)));
                    }
                }
                if (done) {
                    rolling = false;
                    b.generateButton.setEnabled(true);
                    b.digitRow.performHapticFeedback(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                            ? HapticFeedbackConstants.CONFIRM : HapticFeedbackConstants.VIRTUAL_KEY);
                    showFinal(r);
                } else {
                    handler.postDelayed(this, 45);
                }
            }
        };
        handler.post(tick);
    }

    // ---------------------------------------------------------------- copy / hide

    private void copy() {
        if (current == null) {
            return;
        }
        ClipboardManager cm = ContextCompat.getSystemService(this, ClipboardManager.class);
        if (cm == null) {
            return;
        }
        ClipData clip = ClipData.newPlainText("MPIN", current.mpin);
        // Android 13+: keep the MPIN out of the clipboard preview.
        PersistableBundle extras = new PersistableBundle();
        extras.putBoolean("android.content.extra.IS_SENSITIVE", true);
        clip.getDescription().setExtras(extras);
        cm.setPrimaryClip(clip);
        Snackbar.make(b.getRoot(), R.string.copied, Snackbar.LENGTH_SHORT).show();

        handler.postDelayed(() -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                cm.clearPrimaryClip();
            } else {
                cm.setPrimaryClip(ClipData.newPlainText("", ""));
            }
        }, CLIPBOARD_CLEAR_MS);
    }

    private void setHidden(boolean hide) {
        hidden = hide;
        adapter.setHidden(hide);
        updateVisibilityButton();
        if (current != null && !rolling) {
            showFinal(current);
        }
    }

    private void updateVisibilityButton() {
        b.visibilityButton.setIconResource(hidden ? R.drawable.ic_eye_off : R.drawable.ic_eye);
        b.visibilityButton.setContentDescription(getString(hidden ? R.string.show : R.string.hide));
    }

    // ---------------------------------------------------------------- history

    private void renderHistory() {
        List<MpinRecord> all = repo.snapshot();
        adapter.submit(all);
        int n = all.size();
        b.historyCount.setText(getResources().getQuantityString(R.plurals.history_count, n, n));
        b.historyEmpty.setVisibility(n == 0 ? View.VISIBLE : View.GONE);
        b.historyList.setVisibility(n == 0 ? View.GONE : View.VISIBLE);
        b.exportButton.setEnabled(n > 0);

        // Recent labels as suggestions, newest first.
        Set<String> labels = new LinkedHashSet<>();
        for (int i = all.size() - 1; i >= 0 && labels.size() < 10; i--) {
            labels.add(all.get(i).label);
        }
        b.labelInput.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, new ArrayList<>(labels)));

        if (current != null && repo.find(current.id) == null) {   // history was cleared
            current = null;
            if (!rolling) {
                renderEmptyDigits();
            }
        }
    }

    private void confirmClear() {
        if (repo.snapshot().isEmpty()) {
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.clear_confirm_title)
                .setMessage(R.string.clear_confirm_body)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.clear, (d, w) -> {
                    WorkManager.getInstance(this).cancelAllWorkByTag(EmailWorker.TAG);
                    repo.clear();
                    Snackbar.make(b.getRoot(), R.string.cleared, Snackbar.LENGTH_SHORT).show();
                })
                .show();
    }

    // ---------------------------------------------------------------- Excel

    private void startExport() {
        if (repo.snapshot().isEmpty()) {
            Snackbar.make(b.getRoot(), R.string.nothing_to_export, Snackbar.LENGTH_SHORT).show();
            return;
        }
        AppLock.get(this).ignoreNextBackground();   // the "Save as" screen is another app
        String name = "mpin_data_" + DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now()) + ".xlsx";
        exportLauncher.launch(name);
    }

    private void exportTo(Uri uri) {
        if (uri == null) {
            return;
        }
        List<MpinRecord> records = repo.snapshot();
        io.execute(() -> {
            boolean ok;
            try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                if (out == null) {
                    throw new java.io.IOException("No output stream");
                }
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss");
                List<Object[]> rows = new ArrayList<>();
                int n = 1;
                for (MpinRecord r : records) {
                    rows.add(new Object[]{
                            n++,
                            r.label,
                            r.mpin,
                            r.length + " digit",
                            fmt.format(Instant.ofEpochMilli(r.createdAt).atZone(ZoneId.systemDefault())),
                            emailStatusText(r.emailStatus)});
                }
                XlsxWriter.write(out, "MPINs",
                        new String[]{"#", "Label / Purpose", "MPIN", "Length", "Generated At", "Email"},
                        rows,
                        new int[]{6, 32, 12, 10, 24, 12},
                        new int[]{XlsxWriter.STYLE_CENTER, XlsxWriter.STYLE_NORMAL, XlsxWriter.STYLE_TEXT_CENTER,
                                XlsxWriter.STYLE_CENTER, XlsxWriter.STYLE_NORMAL, XlsxWriter.STYLE_CENTER});
                ok = true;
            } catch (Exception e) {
                ok = false;
            }
            boolean success = ok;
            runOnUiThread(() -> Snackbar.make(b.getRoot(),
                    success ? getString(R.string.exported, records.size()) : getString(R.string.export_failed),
                    Snackbar.LENGTH_LONG).show());
        });
    }

    private static String emailStatusText(MpinRecord.EmailStatus s) {
        switch (s) {
            case SENT: return "Sent";
            case PENDING: return "Pending";
            case FAILED: return "Failed";
            default: return "Off";
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
