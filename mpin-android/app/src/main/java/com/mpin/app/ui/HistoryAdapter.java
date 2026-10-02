package com.mpin.app.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.mpin.app.R;
import com.mpin.app.core.MpinRecord;
import com.mpin.app.databinding.ItemMpinBinding;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** History list, newest first. */
class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.Holder> {

    private static final DateTimeFormatter TODAY = DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault());
    private static final DateTimeFormatter OLDER = DateTimeFormatter.ofPattern("dd MMM, hh:mm a", Locale.getDefault());

    private final List<MpinRecord> items = new ArrayList<>();
    private boolean hidden;

    void submit(List<MpinRecord> oldestFirst) {
        items.clear();
        items.addAll(oldestFirst);
        Collections.reverse(items);
        notifyDataSetChanged();
    }

    void setHidden(boolean hidden) {
        this.hidden = hidden;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemMpinBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        MpinRecord r = items.get(position);
        h.b.label.setText(r.label);
        h.b.pin.setText(hidden ? r.mpin.replaceAll(".", "•") : r.mpin);
        h.b.badge.setText(h.itemView.getContext().getString(
                r.length == 4 ? R.string.digits_4 : R.string.digits_6));

        ZonedDateTime time = Instant.ofEpochMilli(r.createdAt).atZone(ZoneId.systemDefault());
        h.b.time.setText((time.toLocalDate().equals(LocalDate.now()) ? TODAY : OLDER).format(time));

        int icon;
        int tint;
        int description;
        switch (r.emailStatus) {
            case SENT:
                icon = R.drawable.ic_mail_sent; tint = R.color.accent; description = R.string.email_status_sent; break;
            case FAILED:
                icon = R.drawable.ic_mail_failed; tint = R.color.danger; description = R.string.email_status_failed; break;
            case PENDING:
                icon = R.drawable.ic_mail; tint = R.color.muted; description = R.string.email_status_pending; break;
            default:
                icon = 0; tint = 0; description = 0;
        }
        if (icon == 0) {
            h.b.emailStatus.setVisibility(View.GONE);
        } else {
            h.b.emailStatus.setVisibility(View.VISIBLE);
            h.b.emailStatus.setImageResource(icon);
            h.b.emailStatus.setImageTintList(ContextCompat.getColorStateList(h.itemView.getContext(), tint));
            h.b.emailStatus.setContentDescription(h.itemView.getContext().getString(description));
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ItemMpinBinding b;

        Holder(ItemMpinBinding b) {
            super(b.getRoot());
            this.b = b;
        }
    }
}
