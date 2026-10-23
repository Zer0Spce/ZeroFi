package com.zer0spce.zerofi.data

import android.content.Context
import com.zer0spce.zerofi.ui.theme.AccentTheme
import org.json.JSONObject

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
    var keepScreenOn: Boolean
        get() = prefs.getBoolean(KEY_KEEP_SCREEN_ON, false)
        set(value) = prefs.edit().putBoolean(KEY_KEEP_SCREEN_ON, value).apply()
    var autoLaunchOnBoot: Boolean
        get() = prefs.getBoolean(KEY_AUTO_LAUNCH, false)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_LAUNCH, value).apply()
    var hotspotAutomationEnabled: Boolean
        get() = prefs.getBoolean(KEY_HOTSPOT_AUTOMATION, false)
        set(value) = prefs.edit().putBoolean(KEY_HOTSPOT_AUTOMATION, value).apply()
    var startupRouterAutomationEnabled: Boolean
        get() = prefs.getBoolean(KEY_STARTUP_ROUTER_AUTOMATION, false)
        set(value) = prefs.edit().putBoolean(KEY_STARTUP_ROUTER_AUTOMATION, value).apply()
    var routerNotificationEnabled: Boolean
        get() = prefs.getBoolean(KEY_ROUTER_NOTIFICATION, true)
        set(value) = prefs.edit().putBoolean(KEY_ROUTER_NOTIFICATION, value).apply()
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
    var hotspotProfileConfirmed: Boolean
        get() = prefs.getBoolean(KEY_HOTSPOT_PROFILE_CONFIRMED, false)
        set(value) = prefs.edit().putBoolean(KEY_HOTSPOT_PROFILE_CONFIRMED, value).apply()
    var preferredBand: String
        get() = prefs.getString(KEY_BAND, "System") ?: "System"
        set(value) = prefs.edit().putString(KEY_BAND, value).apply()

    var autoOffMinutes: Int
        get() = prefs.getInt(KEY_AUTO_OFF_MINUTES, 0).coerceIn(0, 120)
        set(value) = prefs.edit().putInt(KEY_AUTO_OFF_MINUTES, value.coerceIn(0, 120)).apply()
    var keepHotspotWhileCharging: Boolean
        get() = prefs.getBoolean(KEY_KEEP_WHILE_CHARGING, true)
        set(value) = prefs.edit().putBoolean(KEY_KEEP_WHILE_CHARGING, value).apply()
    var dataAlertsEnabled: Boolean
        get() = prefs.getBoolean(KEY_DATA_ALERTS, true)
        set(value) = prefs.edit().putBoolean(KEY_DATA_ALERTS, value).apply()
    var lastDataAlertLevel: Int
        get() = prefs.getInt(KEY_LAST_DATA_ALERT_LEVEL, 0)
        set(value) = prefs.edit().putInt(KEY_LAST_DATA_ALERT_LEVEL, value).apply()
    var thermalAlertsEnabled: Boolean
        get() = prefs.getBoolean(KEY_THERMAL_ALERTS, true)
        set(value) = prefs.edit().putBoolean(KEY_THERMAL_ALERTS, value).apply()
    var thermalThresholdC: Float
        get() = prefs.getFloat(KEY_THERMAL_THRESHOLD, 42f).coerceIn(35f, 55f)
        set(value) = prefs.edit().putFloat(KEY_THERMAL_THRESHOLD, value.coerceIn(35f, 55f)).apply()
    var restoreRouterLock: Boolean
        get() = prefs.getBoolean(KEY_RESTORE_ROUTER_LOCK, false)
        set(value) = prefs.edit().putBoolean(KEY_RESTORE_ROUTER_LOCK, value).apply()
    var autoOledOnStandby: Boolean
        get() = prefs.getBoolean(KEY_AUTO_OLED_STANDBY, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_OLED_STANDBY, value).apply()

    var trustedClientMacs: Set<String>
        get() = prefs.getStringSet(KEY_TRUSTED_CLIENTS, emptySet())?.toSet() ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_TRUSTED_CLIENTS, value.map(String::lowercase).toSet()).apply()

    fun clientAlias(mac: String): String? = runCatching {
        JSONObject(prefs.getString(KEY_CLIENT_ALIASES, "{}") ?: "{}").optString(mac.lowercase()).takeIf { it.isNotBlank() }
    }.getOrNull()

    fun setClientAlias(mac: String, alias: String) {
        val objectValue = runCatching { JSONObject(prefs.getString(KEY_CLIENT_ALIASES, "{}") ?: "{}") }.getOrDefault(JSONObject())
        if (alias.isBlank()) objectValue.remove(mac.lowercase()) else objectValue.put(mac.lowercase(), alias.trim().take(40))
        prefs.edit().putString(KEY_CLIENT_ALIASES, objectValue.toString()).apply()
    }

    fun exportJson(): String = JSONObject().apply {
        put("schema", 3)
        put("darkMode", darkMode)
        put("oledSaver", oledSaver)
        put("dataLimitGb", dataLimitGb.toDouble())
        put("cycleResetDay", cycleResetDay)
        put("keepScreenOn", keepScreenOn)
        put("autoLaunchOnBoot", autoLaunchOnBoot)
        put("hotspotAutomationEnabled", hotspotAutomationEnabled)
        put("startupRouterAutomationEnabled", startupRouterAutomationEnabled)
        put("routerNotificationEnabled", routerNotificationEnabled)
        put("immersiveMode", immersiveMode)
        put("dedicatedBatterySaver", dedicatedBatterySaver)
        put("hotspotSsid", hotspotSsid)
        put("hotspotPassword", hotspotPassword)
        put("hotspotSecurity", hotspotSecurity)
        put("preferredBand", preferredBand)
        put("autoOffMinutes", autoOffMinutes)
        put("keepHotspotWhileCharging", keepHotspotWhileCharging)
        put("dataAlertsEnabled", dataAlertsEnabled)
        put("thermalAlertsEnabled", thermalAlertsEnabled)
        put("thermalThresholdC", thermalThresholdC.toDouble())
        put("restoreRouterLock", restoreRouterLock)
        put("autoOledOnStandby", autoOledOnStandby)
        put("trustedClients", trustedClientMacs.joinToString(","))
        put("clientAliases", prefs.getString(KEY_CLIENT_ALIASES, "{}") ?: "{}")
    }.toString(2)

    fun importJson(raw: String): Boolean = runCatching {
        val j = JSONObject(raw)
        darkMode = j.optBoolean("darkMode", darkMode)
        oledSaver = j.optBoolean("oledSaver", oledSaver)
        dataLimitGb = j.optDouble("dataLimitGb", dataLimitGb.toDouble()).toFloat()
        cycleResetDay = j.optInt("cycleResetDay", cycleResetDay)
        keepScreenOn = j.optBoolean("keepScreenOn", keepScreenOn)
        autoLaunchOnBoot = j.optBoolean("autoLaunchOnBoot", autoLaunchOnBoot)
        hotspotAutomationEnabled = j.optBoolean("hotspotAutomationEnabled", hotspotAutomationEnabled)
        startupRouterAutomationEnabled = j.optBoolean("startupRouterAutomationEnabled", startupRouterAutomationEnabled)
        routerNotificationEnabled = j.optBoolean("routerNotificationEnabled", routerNotificationEnabled)
        immersiveMode = j.optBoolean("immersiveMode", immersiveMode)
        dedicatedBatterySaver = j.optBoolean("dedicatedBatterySaver", dedicatedBatterySaver)
        hotspotSsid = j.optString("hotspotSsid", hotspotSsid)
        hotspotPassword = j.optString("hotspotPassword", hotspotPassword)
        hotspotSecurity = j.optString("hotspotSecurity", hotspotSecurity)
        preferredBand = j.optString("preferredBand", preferredBand)
        autoOffMinutes = j.optInt("autoOffMinutes", autoOffMinutes)
        keepHotspotWhileCharging = j.optBoolean("keepHotspotWhileCharging", keepHotspotWhileCharging)
        dataAlertsEnabled = j.optBoolean("dataAlertsEnabled", dataAlertsEnabled)
        thermalAlertsEnabled = j.optBoolean("thermalAlertsEnabled", thermalAlertsEnabled)
        thermalThresholdC = j.optDouble("thermalThresholdC", thermalThresholdC.toDouble()).toFloat()
        restoreRouterLock = j.optBoolean("restoreRouterLock", restoreRouterLock)
        autoOledOnStandby = j.optBoolean("autoOledOnStandby", autoOledOnStandby)
        trustedClientMacs = j.optString("trustedClients", "").split(',').map { it.trim().lowercase() }.filter { it.isNotBlank() }.toSet()
        val aliases = j.optString("clientAliases", "{}")
        JSONObject(aliases)
        prefs.edit().putString(KEY_CLIENT_ALIASES, aliases).apply()
        true
    }.getOrDefault(false)

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
        private const val KEY_STARTUP_ROUTER_AUTOMATION = "startup_router_automation_enabled"
        private const val KEY_ROUTER_NOTIFICATION = "router_notification_enabled"
        private const val KEY_IMMERSIVE = "immersive_mode"
        private const val KEY_DEDICATED_BATTERY_SAVER = "dedicated_battery_saver"
        private const val KEY_HOTSPOT_SSID = "hotspot_ssid"
        private const val KEY_HOTSPOT_PASSWORD = "hotspot_password"
        private const val KEY_HOTSPOT_SECURITY = "hotspot_security"
        private const val KEY_HOTSPOT_PROFILE_CONFIRMED = "hotspot_profile_confirmed"
        private const val KEY_BAND = "preferred_band"
        private const val KEY_AUTO_OFF_MINUTES = "auto_off_minutes"
        private const val KEY_KEEP_WHILE_CHARGING = "keep_hotspot_while_charging"
        private const val KEY_DATA_ALERTS = "data_alerts_enabled"
        private const val KEY_LAST_DATA_ALERT_LEVEL = "last_data_alert_level"
        private const val KEY_THERMAL_ALERTS = "thermal_alerts_enabled"
        private const val KEY_THERMAL_THRESHOLD = "thermal_threshold_c"
        private const val KEY_RESTORE_ROUTER_LOCK = "restore_router_lock"
        private const val KEY_AUTO_OLED_STANDBY = "auto_oled_standby"
        private const val KEY_TRUSTED_CLIENTS = "trusted_clients"
        private const val KEY_CLIENT_ALIASES = "client_aliases"
    }
}
