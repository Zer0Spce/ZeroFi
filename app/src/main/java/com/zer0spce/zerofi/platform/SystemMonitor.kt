package com.zer0spce.zerofi.platform

import android.Manifest
import android.app.AppOpsManager
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Process
import android.os.SystemClock
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.ZonedDateTime


data class SystemSnapshot(
    val carrierName: String = "Mobile network",
    val networkType: String = "—",
    val signalLevel: Int? = null,
    val signalDbm: Int? = null,
    val batteryPercent: Int? = null,
    val batteryTempC: Float? = null,
    val charging: Boolean = false,
    val cellularDefault: Boolean = false,
    val validatedInternet: Boolean = false,
    val monthBytes: Long? = null,
    val threeDayBytes: Long? = null,
    val usageAccessGranted: Boolean = false,
    val phonePermissionGranted: Boolean = false,
    val uptimeText: String = "—",
)

class SystemMonitor(private val context: Context) {
    private val appContext = context.applicationContext
    private var lastUsageReadAt = 0L
    private var lastUsageResetDay = -1
    private var cachedMonthBytes: Long? = null
    private var cachedThreeDayBytes: Long? = null

    suspend fun snapshot(resetDay: Int): SystemSnapshot = withContext(Dispatchers.IO) {
        val phonePermission = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.READ_PHONE_STATE,
        ) == PackageManager.PERMISSION_GRANTED

        val telephony = appContext.getSystemService(TelephonyManager::class.java)
        val connectivity = appContext.getSystemService(ConnectivityManager::class.java)
        val batteryIntent = appContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        val batteryLevel = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val batteryScale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val batteryPercent = if (batteryLevel >= 0 && batteryScale > 0) {
            (batteryLevel * 100f / batteryScale).toInt().coerceIn(0, 100)
        } else {
            null
        }
        val batteryTempRaw = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
        val batteryTemp = batteryTempRaw?.takeIf { it != Int.MIN_VALUE }?.div(10f)
        val batteryStatus = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val charging = batteryStatus == BatteryManager.BATTERY_STATUS_CHARGING ||
            batteryStatus == BatteryManager.BATTERY_STATUS_FULL

        val activeNetwork = connectivity.activeNetwork
        val caps = activeNetwork?.let(connectivity::getNetworkCapabilities)
        val cellularDefault = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
        val validated = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true

        val carrier = runCatching { telephony.networkOperatorName }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: "Mobile network"

        val networkType = if (phonePermission) {
            runCatching { networkTypeLabel(telephony.dataNetworkType) }.getOrDefault("—")
        } else {
            "Permission needed"
        }

        val signalStrength = runCatching { telephony.signalStrength }.getOrNull()
        val signalLevel = signalStrength?.level?.coerceIn(0, 4)
        val signalDbm = signalStrength?.cellSignalStrengths?.firstOrNull()?.dbm

        val usageAccess = hasUsageAccess()
        if (usageAccess) {
            refreshUsageIfNeeded(resetDay)
        } else {
            cachedMonthBytes = null
            cachedThreeDayBytes = null
        }

        SystemSnapshot(
            carrierName = carrier,
            networkType = networkType,
            signalLevel = signalLevel,
            signalDbm = signalDbm,
            batteryPercent = batteryPercent,
            batteryTempC = batteryTemp,
            charging = charging,
            cellularDefault = cellularDefault,
            validatedInternet = validated,
            monthBytes = cachedMonthBytes,
            threeDayBytes = cachedThreeDayBytes,
            usageAccessGranted = usageAccess,
            phonePermissionGranted = phonePermission,
            uptimeText = formatUptime(SystemClock.elapsedRealtime()),
        )
    }

    private fun hasUsageAccess(): Boolean {
        val appOps = appContext.getSystemService(AppOpsManager::class.java)
        return appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            appContext.packageName,
        ) == AppOpsManager.MODE_ALLOWED
    }

    @Suppress("DEPRECATION")
    private fun refreshUsageIfNeeded(resetDay: Int) {
        val nowElapsed = SystemClock.elapsedRealtime()
        if (resetDay == lastUsageResetDay && nowElapsed - lastUsageReadAt < USAGE_CACHE_MS) return

        lastUsageReadAt = nowElapsed
        lastUsageResetDay = resetDay

        val now = System.currentTimeMillis()
        val manager = appContext.getSystemService(NetworkStatsManager::class.java)
        cachedMonthBytes = queryMobileBytes(manager, cycleStartMillis(resetDay), now)
        cachedThreeDayBytes = queryMobileBytes(
            manager,
            now - Duration.ofDays(3).toMillis(),
            now,
        )
    }

    @Suppress("DEPRECATION")
    private fun queryMobileBytes(
        manager: NetworkStatsManager,
        start: Long,
        end: Long,
    ): Long? = runCatching {
        val bucket = manager.querySummaryForDevice(
            ConnectivityManager.TYPE_MOBILE,
            null,
            start,
            end,
        )
        bucket.rxBytes + bucket.txBytes
    }.getOrNull()

    private fun cycleStartMillis(resetDay: Int): Long {
        val day = resetDay.coerceIn(1, 28)
        val now = ZonedDateTime.now()
        var start = now.withDayOfMonth(day).toLocalDate().atStartOfDay(now.zone)
        if (start.isAfter(now)) start = start.minusMonths(1)
        return start.toInstant().toEpochMilli()
    }

    private fun networkTypeLabel(type: Int): String = when (type) {
        TelephonyManager.NETWORK_TYPE_NR -> "5G"
        TelephonyManager.NETWORK_TYPE_LTE -> "4G LTE"
        TelephonyManager.NETWORK_TYPE_HSPAP,
        TelephonyManager.NETWORK_TYPE_HSPA,
        TelephonyManager.NETWORK_TYPE_HSDPA,
        TelephonyManager.NETWORK_TYPE_HSUPA,
        TelephonyManager.NETWORK_TYPE_UMTS -> "3G"
        TelephonyManager.NETWORK_TYPE_EDGE,
        TelephonyManager.NETWORK_TYPE_GPRS,
        TelephonyManager.NETWORK_TYPE_GSM -> "2G"
        TelephonyManager.NETWORK_TYPE_IWLAN -> "Wi-Fi calling"
        TelephonyManager.NETWORK_TYPE_UNKNOWN -> "Unknown"
        else -> "Mobile"
    }

    private fun formatUptime(elapsedMs: Long): String {
        val totalMinutes = elapsedMs / 60_000L
        val days = totalMinutes / (24 * 60)
        val hours = (totalMinutes / 60) % 24
        val minutes = totalMinutes % 60
        return if (days > 0) {
            "${days}d ${hours}h ${minutes}m"
        } else {
            "${hours}h ${minutes}m"
        }
    }

    companion object {
        private const val USAGE_CACHE_MS = 60_000L
    }
}
