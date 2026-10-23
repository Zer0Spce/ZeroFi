package com.zer0spce.zerofi.data

import android.content.Context

data class RouterRulesSnapshot(
    val quietHoursEnabled: Boolean = false,
    val quietStartHour: Int = 23,
    val quietEndHour: Int = 6,
    val dimWhileCharging: Boolean = false,
)

class RouterRulesStore(context: Context) {
    private val prefs = context.getSharedPreferences("zerofi_router_rules", Context.MODE_PRIVATE)

    var quietHoursEnabled: Boolean
        get() = prefs.getBoolean("quiet_enabled", false)
        set(value) = prefs.edit().putBoolean("quiet_enabled", value).apply()

    var quietStartHour: Int
        get() = prefs.getInt("quiet_start", 23).coerceIn(0, 23)
        set(value) = prefs.edit().putInt("quiet_start", value.coerceIn(0, 23)).apply()

    var quietEndHour: Int
        get() = prefs.getInt("quiet_end", 6).coerceIn(0, 23)
        set(value) = prefs.edit().putInt("quiet_end", value.coerceIn(0, 23)).apply()

    var dimWhileCharging: Boolean
        get() = prefs.getBoolean("dim_while_charging", false)
        set(value) = prefs.edit().putBoolean("dim_while_charging", value).apply()

    fun snapshot() = RouterRulesSnapshot(
        quietHoursEnabled = quietHoursEnabled,
        quietStartHour = quietStartHour,
        quietEndHour = quietEndHour,
        dimWhileCharging = dimWhileCharging,
    )
}
