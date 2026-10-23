package com.zer0spce.zerofi.platform

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.zer0spce.zerofi.R

object RouterAlertNotifier {
    private const val CHANNEL_ID = "zerofi_router_alerts"

    fun notificationsGranted(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED

    fun notifyDataUsage(context: Context, percent: Int, usedText: String, limitText: String) {
        notify(
            context,
            1100 + percent,
            "ZeroFi data usage: $percent%",
            "$usedText used of $limitText allowance.",
        )
    }

    fun notifyThermal(context: Context, temperatureC: Float) {
        notify(
            context,
            2001,
            "ZeroFi temperature warning",
            "Battery temperature is %.1f°C. Improve airflow or reduce load.".format(temperatureC),
        )
    }

    fun notifyAutoOff(context: Context, minutes: Int) {
        notify(
            context,
            3001,
            "Hotspot turned off",
            "ZeroFi detected no clients for $minutes minutes and turned the hotspot off.",
        )
    }

    private fun notify(context: Context, id: Int, title: String, text: String) {
        if (!notificationsGranted(context)) return
        val manager = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "ZeroFi router alerts",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = "Data, hotspot and device temperature alerts"
                }
            )
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .build()
        manager.notify(id, notification)
    }
}
