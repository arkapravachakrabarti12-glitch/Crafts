package com.crafts.powerscheduler

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * Turns the screen on (even over the lock screen) so the power menu can be
 * shown, then asks [PowerOffService] to tap through it.
 */
class WakeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        setContentView(TextView(this).apply {
            setText(R.string.powering_off)
            textSize = 22f
            gravity = android.view.Gravity.CENTER
        })

        Handler(Looper.getMainLooper()).postDelayed({
            val service = PowerOffService.instance
            if (service == null) {
                PowerAlarmReceiver.notifyFailure(this)
                finish()
            } else {
                service.startPowerOff {
                    PowerAlarmReceiver.notifyFailure(this)
                    finish()
                }
            }
        }, 800)
    }
}
