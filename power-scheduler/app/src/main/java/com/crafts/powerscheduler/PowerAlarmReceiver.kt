package com.crafts.powerscheduler

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.text.format.DateFormat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import java.util.Date
import kotlin.concurrent.thread

/**
 * Power-off flow: ACTION_WARN shows a cancellable 60 s countdown, then
 * ACTION_SHUTDOWN turns the phone off unless ACTION_CANCEL arrived first.
 */
class PowerAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_WARN -> {
                AlarmScheduler.scheduleShutdown(context)
                showWarning(context)
                // Queue tomorrow's (or the next selected day's) run. Skip ahead a
                // minute so an alarm delivered a hair early can't re-pick today.
                AlarmScheduler.scheduleAll(context, System.currentTimeMillis() + 60_000)
            }
            ACTION_CANCEL -> {
                AlarmScheduler.cancelShutdown(context)
                NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
                notify(
                    context,
                    NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_power)
                        .setContentTitle(context.getString(R.string.notif_cancelled))
                        .setTimeoutAfter(5_000)
                )
            }
            ACTION_SHUTDOWN -> {
                val pending = goAsync()
                thread {
                    try {
                        powerOff(context, AlarmScheduler.nextPowerOn(context))
                    } finally {
                        pending.finish()
                    }
                }
            }
        }
    }

    private fun showWarning(context: Context) {
        val wakeAt = AlarmScheduler.nextPowerOn(context)
        val text = if (wakeAt != null) {
            context.getString(R.string.notif_warn_text_wake, formatTime(context, wakeAt))
        } else {
            context.getString(R.string.notif_warn_text)
        }
        val cancel = PendingIntent.getBroadcast(
            context, 10,
            Intent(context, PowerAlarmReceiver::class.java).setAction(ACTION_CANCEL),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        notify(
            context,
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_power)
                .setContentTitle(context.getString(R.string.notif_warn_title))
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setWhen(System.currentTimeMillis() + AlarmScheduler.WARNING_SECONDS * 1000)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setOngoing(true)
                .setContentIntent(AlarmScheduler.showAppIntent(context))
                .addAction(0, context.getString(R.string.cancel), cancel)
        )
    }

    companion object {
        const val ACTION_WARN = "com.crafts.powerscheduler.WARN"
        const val ACTION_CANCEL = "com.crafts.powerscheduler.CANCEL"
        const val ACTION_SHUTDOWN = "com.crafts.powerscheduler.SHUTDOWN"

        private const val CHANNEL_ID = "power"
        private const val NOTIFICATION_ID = 1

        /**
         * Blocks; call off the main thread. Uses root when available, otherwise
         * the accessibility service. Posts an error notification if neither works.
         */
        fun powerOff(context: Context, wakeAt: Long?) {
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
            if (RootShell.shutdown(wakeAt).exitCode == 0) return
            if (PowerOffService.instance != null) {
                // An enabled accessibility service lets us start an activity from the background.
                context.startActivity(
                    Intent(context, WakeActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                return
            }
            notifyFailure(context)
        }

        fun notifyFailure(context: Context) {
            notify(
                context,
                NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_power)
                    .setContentTitle(context.getString(R.string.notif_failed_title))
                    .setContentText(context.getString(R.string.notif_failed_text))
                    .setContentIntent(AlarmScheduler.showAppIntent(context))
                    .setAutoCancel(true)
            )
        }

        fun ensureChannel(context: Context) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_HIGH,
            )
            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }

        private fun notify(context: Context, builder: NotificationCompat.Builder) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) return
            ensureChannel(context)
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
        }

        fun formatTime(context: Context, millis: Long): String {
            val date = Date(millis)
            return DateFormat.getMediumDateFormat(context).format(date) + " " +
                DateFormat.getTimeFormat(context).format(date)
        }
    }
}
