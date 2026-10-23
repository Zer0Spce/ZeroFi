package com.zer0spce.zerofi.platform

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.zer0spce.zerofi.MainActivity
import java.util.Locale

object RouterStatusNotifier {
    const val ACTION_TOGGLE_HOTSPOT = "com.zer0spce.zerofi.action.TOGGLE_HOTSPOT"
    private const val CHANNEL_ID = "zerofi_router_status"
    private const val NOTIFICATION_ID = 2301

    fun update(context: Context, snapshot: SystemSnapshot, hotspotActive: Boolean?, clients: HotspotClientSnapshot, enabled: Boolean) {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (!enabled) {
            manager.cancel(NOTIFICATION_ID)
            return
        }
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        ensureChannel(manager)

        val openIntent = Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val openPending = PendingIntent.getActivity(context, 0, openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val togglePending = PendingIntent.getBroadcast(
            context,
            1,
            Intent(context, RouterActionReceiver::class.java).setAction(ACTION_TOGGLE_HOTSPOT),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val clientText = if (clients.available) "${clients.clients.size} clients" else "clients restricted"
        val dataText = snapshot.monthBytes?.let(::formatBytesShort) ?: "data —"
        val battery = snapshot.batteryPercent?.let { "$it%" } ?: "—"
        val title = "ZeroFi · ${snapshot.networkType} · ${if (hotspotActive == true) "Hotspot ON" else "Hotspot OFF"}"
        val text = "$clientText · $battery battery · $dataText"

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Notification.Builder(context, CHANNEL_ID) else Notification.Builder(context)
        val notification = builder
            .setSmallIcon(android.R.drawable.stat_notify_sync_noanim)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(Notification.BigTextStyle().bigText("$text\n${snapshot.signalDbm?.let { "$it dBm" } ?: snapshot.carrierName} · ↓ ${rate(snapshot.downloadBytesPerSecond)} · ↑ ${rate(snapshot.uploadBytesPerSecond)}"))
            .setContentIntent(openPending)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(Notification.CATEGORY_STATUS)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .addAction(android.R.drawable.ic_menu_manage, if (hotspotActive == true) "Turn hotspot off" else "Turn hotspot on", togglePending)
            .build()
        manager.notify(NOTIFICATION_ID, notification)
    }

    fun cancel(context: Context) {
        context.getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
    }

    private fun ensureChannel(manager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Router status", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Persistent ZeroFi hotspot, signal, data and battery status"
                    setShowBadge(false)
                },
            )
        }
    }

    private fun rate(value: Long?): String = when {
        value == null -> "—"
        value >= 1_048_576 -> String.format(Locale.US, "%.1f MB/s", value / 1_048_576.0)
        value >= 1024 -> String.format(Locale.US, "%.0f KB/s", value / 1024.0)
        else -> "$value B/s"
    }

    private fun formatBytesShort(bytes: Long): String = when {
        bytes >= 1_073_741_824L -> String.format(Locale.US, "%.2f GB", bytes / 1_073_741_824.0)
        bytes >= 1_048_576L -> String.format(Locale.US, "%.1f MB", bytes / 1_048_576.0)
        bytes >= 1024 -> String.format(Locale.US, "%.0f KB", bytes / 1024.0)
        else -> "$bytes B"
    }
}
