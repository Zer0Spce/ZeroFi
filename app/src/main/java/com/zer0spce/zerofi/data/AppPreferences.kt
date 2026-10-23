package com.zer0spce.zerofi.data

import android.content.Context
import com.zer0spce.zerofi.ui.theme.AccentTheme

class AppPreferences(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var accentTheme: AccentTheme
        get() = runCatching { AccentTheme.valueOf(prefs.getString(KEY_ACCENT, AccentTheme.SUNSET_ORANGE.name)!!) }
            .getOrDefault(AccentTheme.SUNSET_ORANGE)
        set(value) = prefs.edit().putString(KEY_ACCENT, value.name).apply()

    var darkMode: Boolean
        get() = prefs.getBoolean(KEY_DARK_MODE, true)
        set(value) = prefs.edit().putBoolean(KEY_DARK_MODE, value).apply()

    var oledSaver: Boolean
        get() = prefs.getBoolean(KEY_OLED_SAVER, false)
        set(value) = prefs.edit().putBoolean(KEY_OLED_SAVER, value).apply()

    var dataLimitGb: Float
        get() = prefs.getFloat(KEY_DATA_LIMIT_GB, 50f)
        set(value) = prefs.edit().putFloat(KEY_DATA_LIMIT_GB, value.coerceIn(1f, 9999f)).apply()

    var cycleResetDay: Int
        get() = prefs.getInt(KEY_CYCLE_DAY, 1).coerceIn(1, 28)
        set(value) = prefs.edit().putInt(KEY_CYCLE_DAY, value.coerceIn(1, 28)).apply()

    // Default OFF so a spare router phone is allowed to sleep and save battery.
    var keepScreenOn: Boolean
        get() = prefs.getBoolean(KEY_KEEP_SCREEN_ON, false)
        set(value) = prefs.edit().putBoolean(KEY_KEEP_SCREEN_ON, value).apply()

    var autoLaunchOnBoot: Boolean
        get() = prefs.getBoolean(KEY_AUTO_LAUNCH, false)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_LAUNCH, value).apply()

    // Opt-in only. ZeroFi never pushes Accessibility setup on first launch.
    var hotspotAutomationEnabled: Boolean
        get() = prefs.getBoolean(KEY_HOTSPOT_AUTOMATION, false)
        set(value) = prefs.edit().putBoolean(KEY_HOTSPOT_AUTOMATION, value).apply()

    var immersiveMode: Boolean
        get() = prefs.getBoolean(KEY_IMMERSIVE, true)
        set(value) = prefs.edit().putBoolean(KEY_IMMERSIVE, value).apply()

    var dedicatedBatterySaver: Boolean
        get() = prefs.getBoolean(KEY_DEDICATED_BATTERY_SAVER, false)
        set(value) = prefs.edit().putBoolean(KEY_DEDICATED_BATTERY_SAVER, value).apply()

    var hotspotSsid: String
        get() = prefs.getString(KEY_HOTSPOT_SSID, "ZeroFi") ?: "ZeroFi"
        set(value) = prefs.edit().putString(KEY_HOTSPOT_SSID, value.trim().take(32)).apply()

    var hotspotPassword: String
        get() = prefs.getString(KEY_HOTSPOT_PASSWORD, "") ?: ""
        set(value) = prefs.edit().putString(KEY_HOTSPOT_PASSWORD, value.take(63)).apply()

    var hotspotSecurity: String
        get() = prefs.getString(KEY_HOTSPOT_SECURITY, "WPA") ?: "WPA"
        set(value) = prefs.edit().putString(KEY_HOTSPOT_SECURITY, value).apply()

    var preferredBand: String
        get() = prefs.getString(KEY_BAND, "System") ?: "System"
        set(value) = prefs.edit().putString(KEY_BAND, value).apply()

    companion object {
        private const val PREFS_NAME = "zerofi_preferences"
        private const val KEY_ACCENT = "accent_theme"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_OLED_SAVER = "oled_saver"
        private const val KEY_DATA_LIMIT_GB = "data_limit_gb"
        private const val KEY_CYCLE_DAY = "cycle_reset_day"
        private const val KEY_KEEP_SCREEN_ON = "keep_screen_on"
        private const val KEY_AUTO_LAUNCH = "auto_launch_on_boot"
        private const val KEY_HOTSPOT_AUTOMATION = "hotspot_automation_enabled"
        private const val KEY_IMMERSIVE = "immersive_mode"
        private const val KEY_DEDICATED_BATTERY_SAVER = "dedicated_battery_saver"
        private const val KEY_HOTSPOT_SSID = "hotspot_ssid"
        private const val KEY_HOTSPOT_PASSWORD = "hotspot_password"
        private const val KEY_HOTSPOT_SECURITY = "hotspot_security"
        private const val KEY_BAND = "preferred_band"
    }
}
