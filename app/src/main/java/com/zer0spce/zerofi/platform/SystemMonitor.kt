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
import android.net.TrafficStats
import android.os.BatteryManager
import android.os.Build
import android.os.Process
import android.os.SystemClock
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import kotlin.math.abs


data class SystemSnapshot(
    val carrierName: String = "Mobile network",
    val networkType: String = "—",
    val signalLevel: Int? = null,
    val signalDbm: Int? = null,
    val batteryPercent: Int? = null,
    val batteryTempC: Float? = null,
    val charging: Boolean = false,
    val batteryTimeRemainingText: String = "Calculating…",
    val cellularDefault: Boolean = false,
    val validatedInternet: Boolean = false,
    val monthBytes: Long? = null,
    val threeDayBytes: Long? = null,
    val downloadBytesPerSecond: Long? = null,
    val uploadBytesPerSecond: Long? = null,
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
    private var smoothedDischargeUa: Double? = null
    private var lastTrafficAt = 0L
    private var lastMobileRx = -1L
    private var lastMobileTx = -1L

    suspend fun snapshot(resetDay: Int): SystemSnapshot = withContext(Dispatchers.IO) {
        val phonePermission = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.READ_PHONE_STATE,
        ) == PackageManager.PERMISSION_GRANTED

        val telephony = appContext.getSystemService(TelephonyManager::class.java)
        val connectivity = appContext.getSystemService(ConnectivityManager::class.java)
        val batteryManager = appContext.getSystemService(BatteryManager::class.java)
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
        val batteryTimeRemaining = estimateBatteryTimeRemaining(batteryManager, charging)

        val activeNetwork = connectivity.activeNetwork
        val caps = activeNetwork?.let(connectivity::getNetworkCapabilities)
        val cellularDefault = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
        val validated = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true

        val carrier = runCatching { telephony.networkOperatorName }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: "Mobile network"

        val networkType = if (phonePermission) {
            val rawType = runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    telephony.dataNetworkType
                } else {
                    @Suppress("DEPRECATION")
                    telephony.networkType
                }
            }.getOrDefault(TelephonyManager.NETWORK_TYPE_UNKNOWN)
            networkTypeLabel(rawType)
        } else {
            "Permission needed"
        }

        val signalStrength = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            runCatching { telephony.signalStrength }.getOrNull()
        } else {
            null
        }
        val signalLevel = signalStrength?.level?.coerceIn(0, 4)
        val signalDbm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            signalStrength?.cellSignalStrengths?.firstOrNull()?.dbm
        } else {
            null
        }

        val usageAccess = hasUsageAccess()
        if (usageAccess) {
            refreshUsageIfNeeded(resetDay)
        } else {
            cachedMonthBytes = null
            cachedThreeDayBytes = null
        }

        val throughput = readMobileThroughput()

        SystemSnapshot(
            carrierName = carrier,
            networkType = networkType,
            signalLevel = signalLevel,
            signalDbm = signalDbm,
            batteryPercent = batteryPercent,
            batteryTempC = batteryTemp,
            charging = charging,
            batteryTimeRemainingText = batteryTimeRemaining,
            cellularDefault = cellularDefault,
            validatedInternet = validated,
            monthBytes = cachedMonthBytes,
            threeDayBytes = cachedThreeDayBytes,
            downloadBytesPerSecond = throughput.first,
            uploadBytesPerSecond = throughput.second,
            usageAccessGranted = usageAccess,
            phonePermissionGranted = phonePermission,
            uptimeText = formatUptime(SystemClock.elapsedRealtime()),
        )
    }

    private fun readMobileThroughput(): Pair<Long?, Long?> {
        val now = SystemClock.elapsedRealtime()
        val rx = TrafficStats.getMobileRxBytes()
        val tx = TrafficStats.getMobileTxBytes()
        if (rx == TrafficStats.UNSUPPORTED.toLong() || tx == TrafficStats.UNSUPPORTED.toLong()) {
            return null to null
        }

        if (lastTrafficAt <= 0L || lastMobileRx < 0L || lastMobileTx < 0L || now <= lastTrafficAt) {
            lastTrafficAt = now
            lastMobileRx = rx
            lastMobileTx = tx
            return null to null
        }

        val elapsedMs = now - lastTrafficAt
        val down = ((rx - lastMobileRx).coerceAtLeast(0L) * 1000L) / elapsedMs.coerceAtLeast(1L)
        val up = ((tx - lastMobileTx).coerceAtLeast(0L) * 1000L) / elapsedMs.coerceAtLeast(1L)
        lastTrafficAt = now
        lastMobileRx = rx
        lastMobileTx = tx
        return down to up
    }

    private fun estimateBatteryTimeRemaining(
        batteryManager: BatteryManager,
        charging: Boolean,
    ): String {
        if (charging) {
            smoothedDischargeUa = null
            return "Charging"
        }

        val chargeCounter = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
        val averageCurrent = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE)
        val currentNow = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        if (chargeCounter <= 0 || chargeCounter == Int.MIN_VALUE) return "Calculating…"

        val rawCurrent = when {
            averageCurrent != Int.MIN_VALUE && abs(averageCurrent) >= MIN_USEFUL_CURRENT_UA -> abs(averageCurrent).toDouble()
            currentNow != Int.MIN_VALUE && abs(currentNow) >= MIN_USEFUL_CURRENT_UA -> abs(currentNow).toDouble()
            else -> return "Calculating…"
        }

        val smoothed = smoothedDischargeUa?.let { previous ->
            previous * 0.75 + rawCurrent * 0.25
        } ?: rawCurrent
        smoothedDischargeUa = smoothed

        val hours = chargeCounter.toDouble() / smoothed
        if (!hours.isFinite() || hours < 0.08 || hours > 96.0) return "Calculating…"
        return formatRemainingHours(hours)
    }

    private fun formatRemainingHours(hours: Double): String {
        val totalMinutes = (hours * 60.0).toInt().coerceAtLeast(1)
        val h = totalMinutes / 60
        val m = totalMinutes % 60
        return when {
            h >= 24 -> "~${h / 24}d ${h % 24}h left"
            h > 0 -> "~${h}h ${m}m left"
            else -> "~${m}m left"
        }
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
        cachedThreeDayBytes = queryMobileBytes(manager, now - THREE_DAYS_MS, now)
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
        val calendar = Calendar.getInstance()
        if (calendar.get(Calendar.DAY_OF_MONTH) < day) calendar.add(Calendar.MONTH, -1)
        calendar.set(Calendar.DAY_OF_MONTH, day)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
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
        return if (days > 0) "${days}d ${hours}h ${minutes}m" else "${hours}h ${minutes}m"
    }

    companion object {
        private const val USAGE_CACHE_MS = 60_000L
        private const val THREE_DAYS_MS = 3L * 24L * 60L * 60L * 1000L
        private const val MIN_USEFUL_CURRENT_UA = 5_000
    }
}
