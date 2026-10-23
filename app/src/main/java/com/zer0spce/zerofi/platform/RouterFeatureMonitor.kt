package com.zer0spce.zerofi.platform

import android.Manifest
import android.app.AppOpsManager
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Process
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

data class RouterFeatureSnapshot(
    val todayBytes: Long? = null,
    val sevenDayBytes: Long? = null,
    val localIp: String = "Unavailable",
    val gatewayIp: String = "Unavailable",
    val vpnActive: Boolean = false,
    val metered: Boolean = true,
    val downstreamKbps: Int? = null,
    val upstreamKbps: Int? = null,
    val deviceName: String = "Android device",
    val androidVersion: String = Build.VERSION.RELEASE,
)

class RouterFeatureMonitor(context: Context) {
    private val appContext = context.applicationContext
    private var lastUsageReadAt = 0L
    private var cachedToday: Long? = null
    private var cachedSeven: Long? = null

    suspend fun snapshot(): RouterFeatureSnapshot = withContext(Dispatchers.IO) {
        val connectivity = appContext.getSystemService(ConnectivityManager::class.java)
        val network = connectivity.activeNetwork
        val caps = network?.let(connectivity::getNetworkCapabilities)
        val link = network?.let(connectivity::getLinkProperties)

        if (hasUsageAccess() && android.os.SystemClock.elapsedRealtime() - lastUsageReadAt > 60_000L) {
            val manager = appContext.getSystemService(NetworkStatsManager::class.java)
            val now = System.currentTimeMillis()
            cachedToday = queryMobileBytes(manager, startOfToday(), now)
            cachedSeven = queryMobileBytes(manager, now - SEVEN_DAYS_MS, now)
            lastUsageReadAt = android.os.SystemClock.elapsedRealtime()
        }

        val localIp = link?.linkAddresses
            ?.mapNotNull { it.address?.hostAddress }
            ?.firstOrNull { it.contains('.') && !it.startsWith("127.") }
            ?: "Unavailable"
        val gateway = link?.routes
            ?.mapNotNull { it.gateway?.hostAddress }
            ?.firstOrNull { it.contains('.') }
            ?: "Unavailable"

        RouterFeatureSnapshot(
            todayBytes = cachedToday,
            sevenDayBytes = cachedSeven,
            localIp = localIp,
            gatewayIp = gateway,
            vpnActive = caps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true,
            metered = runCatching { connectivity.isActiveNetworkMetered }.getOrDefault(true),
            downstreamKbps = caps?.linkDownstreamBandwidthKbps?.takeIf { it > 0 },
            upstreamKbps = caps?.linkUpstreamBandwidthKbps?.takeIf { it > 0 },
            deviceName = listOf(Build.MANUFACTURER, Build.MODEL)
                .filter { it.isNotBlank() }
                .joinToString(" ")
                .ifBlank { "Android device" },
            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
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
    private fun queryMobileBytes(manager: NetworkStatsManager, start: Long, end: Long): Long? = runCatching {
        val bucket = manager.querySummaryForDevice(ConnectivityManager.TYPE_MOBILE, null, start, end)
        bucket.rxBytes + bucket.txBytes
    }.getOrNull()

    private fun startOfToday(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    companion object {
        private const val SEVEN_DAYS_MS = 7L * 24L * 60L * 60L * 1000L
    }
}
