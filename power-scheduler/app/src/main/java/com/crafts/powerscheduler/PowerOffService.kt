package com.crafts.powerscheduler

import android.accessibilityservice.AccessibilityService
import android.content.res.Resources
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.util.Locale

/**
 * No-root power off: opens the system power menu and taps its "Power off"
 * button (and any confirmation button that follows), like a person would.
 * Button labels differ between phone makers, so this works on many phones,
 * not all.
 */
class PowerOffService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var deadline = 0L
    private var clicks = 0
    private var lastClickAt = 0L
    private var onFailure: (() -> Unit)? = null

    override fun onServiceConnected() {
        instance = this
    }

    override fun onDestroy() {
        instance = null
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (deadline != 0L) tryClick()
    }

    override fun onInterrupt() {}

    /** Must be called on the main thread. */
    fun startPowerOff(onFailure: () -> Unit) {
        this.onFailure = onFailure
        deadline = SystemClock.uptimeMillis() + TIMEOUT_MS
        clicks = 0
        lastClickAt = 0L
        performGlobalAction(GLOBAL_ACTION_POWER_DIALOG)
        handler.post(poll)
    }

    private val poll = object : Runnable {
        override fun run() {
            if (deadline == 0L) return
            if (SystemClock.uptimeMillis() > deadline) {
                deadline = 0L
                // We're still running, so the phone didn't turn off.
                performGlobalAction(GLOBAL_ACTION_BACK)
                onFailure?.invoke()
                return
            }
            tryClick()
            handler.postDelayed(this, 400)
        }
    }

    private fun tryClick() {
        // Give each screen (menu, then confirmation) time to appear before tapping again.
        if (SystemClock.uptimeMillis() - lastClickAt < 1_000 || clicks >= MAX_CLICKS) return
        val roots = windows.mapNotNull { it.root } + listOfNotNull(rootInActiveWindow)
        for (root in roots) {
            if (root.packageName == packageName) continue // never tap our own buttons
            val target = findPowerOff(root) ?: continue
            if (target.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                clicks++
                lastClickAt = SystemClock.uptimeMillis()
                return
            }
        }
    }

    private fun findPowerOff(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val label = listOfNotNull(node.text, node.contentDescription)
            .joinToString(" ").lowercase(Locale.getDefault())
        if (label.isNotBlank() && labels.any { label.contains(it) } &&
            excluded.none { label.contains(it) }
        ) {
            clickableAncestor(node)?.let { return it }
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            findPowerOff(child)?.let { return it }
        }
        return null
    }

    private fun clickableAncestor(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var n: AccessibilityNodeInfo? = node
        while (n != null) {
            if (n.isClickable && n.isEnabled) return n
            n = n.parent
        }
        return null
    }

    private val labels: List<String> by lazy {
        val fromSystem = listOf("global_action_power_off", "power_off")
            .mapNotNull { name ->
                val id = Resources.getSystem().getIdentifier(name, "string", "android")
                if (id != 0) Resources.getSystem().getString(id) else null
            }
        (fromSystem + listOf("power off", "power-off", "shut down", "shutdown", "turn off", "switch off"))
            .map { it.lowercase(Locale.getDefault()) }
            .filter { it.isNotBlank() }
            .distinct()
    }

    private val excluded = listOf("restart", "reboot", "emergency", "sos", "airplane", "flight")

    companion object {
        private const val TIMEOUT_MS = 15_000L
        private const val MAX_CLICKS = 3

        /** The running service, or null when it isn't enabled in Accessibility settings. */
        @Volatile
        var instance: PowerOffService? = null
            private set
    }
}
