package com.crafts.powerscheduler

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.format.DateFormat
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.crafts.powerscheduler.databinding.ActivityMainBinding
import com.crafts.powerscheduler.databinding.ScheduleCardBinding
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.DateFormatSymbols
import java.util.Calendar
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bindCard(binding.offCard, ScheduleKind.POWER_OFF, R.string.off_title, R.string.off_subtitle)
        bindCard(binding.onCard, ScheduleKind.POWER_ON, R.string.on_title, R.string.on_subtitle)

        binding.rootCheck.setOnClickListener { checkRoot() }
        binding.testButton.setOnClickListener {
            val wakeAt = System.currentTimeMillis() + 3 * 60 * 1000
            confirmPowerOff { PowerAlarmReceiver.powerOff(applicationContext, wakeAt) }
        }
        binding.testOffButton.setOnClickListener {
            confirmPowerOff { PowerAlarmReceiver.powerOff(applicationContext, null) }
        }
        binding.a11yOpen.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        binding.exactAlarmGrant.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                startActivity(
                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$packageName"))
                )
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        checkRoot()
    }

    override fun onResume() {
        super.onResume()
        binding.exactAlarmBanner.visibility =
            if (AlarmScheduler.canScheduleExact(this)) View.GONE else View.VISIBLE
        binding.a11yStatus.setText(
            if (PowerOffService.instance != null) R.string.a11y_on else R.string.a11y_off
        )
        AlarmScheduler.scheduleAll(this)
    }

    private fun bindCard(card: ScheduleCardBinding, kind: ScheduleKind, title: Int, subtitle: Int) {
        card.title.setText(title)
        card.subtitle.setText(subtitle)

        // Build one chip per weekday, starting on the locale's first day of the week.
        val names = DateFormatSymbols.getInstance().shortWeekdays
        val first = Calendar.getInstance().firstDayOfWeek
        val chips = (0 until 7).map { i ->
            val day = (first - 1 + i) % 7 + 1
            (layoutInflater.inflate(R.layout.day_chip, card.days, false) as Chip).apply {
                text = names[day]
                tag = day
                card.days.addView(this)
            }
        }

        fun render() {
            val s = ScheduleStore.load(this, kind)
            card.enabled.isChecked = s.enabled
            card.timeButton.text = formatClock(s.hour, s.minute)
            chips.forEach { it.isChecked = (it.tag as Int) in s.days }
            card.nextRun.text = when {
                !s.enabled -> getString(R.string.disabled)
                s.days.isEmpty() -> getString(R.string.no_days)
                else -> getString(
                    R.string.next_run,
                    PowerAlarmReceiver.formatTime(this, s.nextTrigger()!!),
                )
            }
        }

        fun update(change: (Schedule) -> Schedule) {
            ScheduleStore.save(this, kind, change(ScheduleStore.load(this, kind)))
            AlarmScheduler.scheduleAll(this)
            render()
        }

        render()

        card.enabled.setOnClickListener { update { it.copy(enabled = card.enabled.isChecked) } }
        card.timeButton.setOnClickListener {
            val s = ScheduleStore.load(this, kind)
            TimePickerDialog(
                this,
                { _, hour, minute -> update { it.copy(hour = hour, minute = minute) } },
                s.hour, s.minute, DateFormat.is24HourFormat(this),
            ).show()
        }
        chips.forEach { chip ->
            chip.setOnClickListener {
                val day = chip.tag as Int
                update { s ->
                    s.copy(days = if (chip.isChecked) s.days + day else s.days - day)
                }
            }
        }
    }

    private fun formatClock(hour: Int, minute: Int): String {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }
        return DateFormat.getTimeFormat(this).format(cal.time)
    }

    private fun checkRoot() {
        binding.rootStatus.setText(R.string.root_checking)
        binding.rootCheck.isEnabled = false
        thread {
            val rooted = RootShell.hasRoot()
            runOnUiThread {
                binding.rootStatus.setText(if (rooted) R.string.root_ok else R.string.root_missing)
                binding.rootCheck.isEnabled = true
            }
        }
    }

    private fun confirmPowerOff(action: () -> Unit) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.test_confirm_title)
            .setMessage(R.string.test_confirm_message)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.test_confirm_ok) { _, _ -> thread { action() } }
            .show()
    }
}
