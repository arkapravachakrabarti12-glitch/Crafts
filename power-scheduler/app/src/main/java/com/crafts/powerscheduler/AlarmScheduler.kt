package com.crafts.powerscheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object AlarmScheduler {
    const val WARNING_SECONDS = 60L

    private const val REQUEST_WARN = 1
    private const val REQUEST_SHUTDOWN = 2
    private const val REQUEST_SHOW = 3

    fun canScheduleExact(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            alarmManager(context).canScheduleExactAlarms()

    /** (Re)schedules the next power-off warning from the saved schedule. */
    fun scheduleAll(context: Context, from: Long = System.currentTimeMillis()) {
        val am = alarmManager(context)
        val warn = receiverIntent(context, PowerAlarmReceiver.ACTION_WARN, REQUEST_WARN)
        am.cancel(warn)

        val off = ScheduleStore.load(context, ScheduleKind.POWER_OFF)
        if (!off.enabled) return
        val at = off.nextTrigger(from) ?: return

        try {
            am.setAlarmClock(AlarmManager.AlarmClockInfo(at, showAppIntent(context)), warn)
        } catch (e: SecurityException) {
            // Exact alarms not allowed; an inexact alarm is better than nothing.
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, warn)
        }
    }

    fun scheduleShutdown(context: Context) {
        val am = alarmManager(context)
        val at = System.currentTimeMillis() + WARNING_SECONDS * 1000
        val pi = receiverIntent(context, PowerAlarmReceiver.ACTION_SHUTDOWN, REQUEST_SHUTDOWN)
        try {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        } catch (e: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }

    fun cancelShutdown(context: Context) {
        alarmManager(context).cancel(
            receiverIntent(context, PowerAlarmReceiver.ACTION_SHUTDOWN, REQUEST_SHUTDOWN)
        )
    }

    /** The time the phone should power back on, if that schedule is enabled. */
    fun nextPowerOn(context: Context): Long? {
        val on = ScheduleStore.load(context, ScheduleKind.POWER_ON)
        return if (on.enabled) on.nextTrigger() else null
    }

    private fun alarmManager(context: Context) =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private fun receiverIntent(context: Context, action: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, PowerAlarmReceiver::class.java).setAction(action)
        return PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    fun showAppIntent(context: Context): PendingIntent = PendingIntent.getActivity(
        context, REQUEST_SHOW, Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}
