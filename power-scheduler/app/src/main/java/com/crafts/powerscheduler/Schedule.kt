package com.crafts.powerscheduler

import android.content.Context
import java.util.Calendar

/** A repeating time of day. [days] holds [Calendar.DAY_OF_WEEK] values. */
data class Schedule(
    val enabled: Boolean,
    val hour: Int,
    val minute: Int,
    val days: Set<Int>,
) {
    /** The first matching moment strictly after [from], or null if no day is selected. */
    fun nextTrigger(from: Long = System.currentTimeMillis()): Long? {
        if (days.isEmpty()) return null
        val cal = Calendar.getInstance().apply {
            timeInMillis = from
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        repeat(8) {
            if (cal.timeInMillis > from && cal.get(Calendar.DAY_OF_WEEK) in days) {
                return cal.timeInMillis
            }
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return null
    }

    companion object {
        val ALL_DAYS = (Calendar.SUNDAY..Calendar.SATURDAY).toSet()
    }
}

enum class ScheduleKind(val key: String, val defaultHour: Int) {
    POWER_OFF("off", 23),
    POWER_ON("on", 7),
}

object ScheduleStore {
    private const val PREFS = "schedules"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(context: Context, kind: ScheduleKind): Schedule {
        val p = prefs(context)
        val k = kind.key
        return Schedule(
            enabled = p.getBoolean("${k}_enabled", false),
            hour = p.getInt("${k}_hour", kind.defaultHour),
            minute = p.getInt("${k}_minute", 0),
            days = p.getStringSet("${k}_days", null)?.map { it.toInt() }?.toSet()
                ?: Schedule.ALL_DAYS,
        )
    }

    fun save(context: Context, kind: ScheduleKind, schedule: Schedule) {
        val k = kind.key
        prefs(context).edit()
            .putBoolean("${k}_enabled", schedule.enabled)
            .putInt("${k}_hour", schedule.hour)
            .putInt("${k}_minute", schedule.minute)
            .putStringSet("${k}_days", schedule.days.map { it.toString() }.toSet())
            .apply()
    }
}
