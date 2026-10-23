package com.zer0spce.zerofi.platform

import android.app.Activity
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.widget.Toast

object HotspotLaunchController {
    private const val PREFS = "zerofi_hotspot_automation"
    private const val KEY_PROMPTED = "accessibility_prompted"
    private const val AUTOMATION_THROTTLE_MS = 8_000L

    @Volatile
    private var lastAutomationRequestElapsed = 0L

    /**
     * Returns true when ZeroFi temporarily opened Accessibility settings for the
     * one-time hotspot automation setup. The caller should avoid entering lock
     * task mode until the user returns.
     */
    fun ensureConfiguredOrRun(activity: Activity): Boolean {
        if (HotspotAutomationService.isEnabled(activity)) {
            val now = SystemClock.elapsedRealtime()
            if (now - lastAutomationRequestElapsed >= AUTOMATION_THROTTLE_MS) {
                lastAutomationRequestElapsed = now
                Handler(Looper.getMainLooper()).postDelayed(
                    { HotspotAutomationService.requestEnsureHotspotOn() },
                    450L,
                )
            }
            return false
        }

        val prefs = activity.getSharedPreferences(PREFS, Activity.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_PROMPTED, false)) {
            prefs.edit().putBoolean(KEY_PROMPTED, true).apply()
            Toast.makeText(
                activity,
                "Enable ZeroFi Hotspot Automation once. After that, hotspot auto-starts with ZeroFi.",
                Toast.LENGTH_LONG,
            ).show()
            runCatching {
                activity.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
            return true
        }

        return false
    }

    fun resetSetupPrompt(activity: Activity) {
        activity.getSharedPreferences(PREFS, Activity.MODE_PRIVATE)
            .edit()
            .remove(KEY_PROMPTED)
            .apply()
        lastAutomationRequestElapsed = 0L
    }
}
