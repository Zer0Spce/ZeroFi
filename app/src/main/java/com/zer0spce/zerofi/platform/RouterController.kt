package com.zer0spce.zerofi.platform

import android.app.Activity
import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.zer0spce.zerofi.admin.ZeroFiDeviceAdminReceiver

object RouterController {
    private const val ACTION_TETHER_SETTINGS = "android.settings.TETHER_SETTINGS"
    private const val ACTION_WIFI_TETHER_SETTINGS = "com.android.settings.WIFI_TETHER_SETTINGS"
    private const val ACTION_SIM_SETTINGS = "android.settings.SIM_SETTINGS"
    private const val ACTION_PRIVATE_DNS_SETTINGS = "android.settings.PRIVATE_DNS_SETTINGS"

    fun openHotspotSettings(context: Context) {
        launchSettings(context, Intent(ACTION_WIFI_TETHER_SETTINGS), Intent(ACTION_TETHER_SETTINGS))
    }

    fun openUsageAccessSettings(context: Context) {
        launchSettings(
            context,
            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply { data = Uri.parse("package:${context.packageName}") },
            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS),
        )
    }

    fun openDataUsageSettings(context: Context) {
        launchSettings(context, Intent(Settings.ACTION_DATA_USAGE_SETTINGS), Intent(Settings.ACTION_SETTINGS))
    }

    fun openMobileNetworkSettings(context: Context) {
        launchSettings(context, Intent(Settings.ACTION_NETWORK_OPERATOR_SETTINGS), Intent(Settings.ACTION_WIRELESS_SETTINGS))
    }

    fun openSimSettings(context: Context) {
        launchSettings(context, Intent(ACTION_SIM_SETTINGS), Intent(Settings.ACTION_WIRELESS_SETTINGS))
    }

    fun openApnSettings(context: Context) {
        launchSettings(context, Intent(Settings.ACTION_APN_SETTINGS), Intent(Settings.ACTION_NETWORK_OPERATOR_SETTINGS))
    }

    fun openVpnSettings(context: Context) {
        launchSettings(context, Intent(Settings.ACTION_VPN_SETTINGS), Intent(Settings.ACTION_WIRELESS_SETTINGS))
    }

    fun openPrivateDnsSettings(context: Context) {
        launchSettings(context, Intent(ACTION_PRIVATE_DNS_SETTINGS), Intent(Settings.ACTION_WIRELESS_SETTINGS))
    }

    fun openBatteryOptimizationSettings(context: Context) {
        launchSettings(
            context,
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
            Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS),
        )
    }

    fun openAppSettings(context: Context) {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        }
    }

    fun shareText(context: Context, title: String, text: String) {
        runCatching {
            context.startActivity(
                Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply {
                        type = "application/json"
                        putExtra(Intent.EXTRA_SUBJECT, title)
                        putExtra(Intent.EXTRA_TEXT, text)
                    },
                    title,
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    private fun launchSettings(context: Context, primary: Intent, fallback: Intent) {
        primary.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(primary) }
            .onFailure { runCatching { context.startActivity(fallback) } }
    }
}

object RouterLockController {
    private fun adminComponent(context: Context) = ComponentName(context, ZeroFiDeviceAdminReceiver::class.java)

    fun isDeviceOwner(context: Context): Boolean = runCatching {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
        dpm?.isDeviceOwnerApp(context.packageName) == true
    }.getOrDefault(false)

    fun isLockTaskRunning(context: Context): Boolean = runCatching {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        activityManager?.lockTaskModeState != null && activityManager.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE
    }.getOrDefault(false)

    fun start(activity: Activity): LockResult {
        val dpm = activity.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
        val owner = runCatching { dpm?.isDeviceOwnerApp(activity.packageName) == true }.getOrDefault(false)
        return runCatching {
            if (owner && dpm != null) {
                dpm.setLockTaskPackages(adminComponent(activity), arrayOf(activity.packageName))
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    dpm.setLockTaskFeatures(adminComponent(activity), DevicePolicyManager.LOCK_TASK_FEATURE_NONE)
                }
            }
            activity.startLockTask()
            LockResult(true, owner, if (owner) "Dedicated Router Lock enabled" else "Screen pinning requested")
        }.getOrElse { LockResult(false, owner, it.message ?: "Unable to start Router Lock") }
    }

    fun stop(activity: Activity): LockResult {
        val owner = isDeviceOwner(activity)
        return runCatching {
            activity.stopLockTask()
            LockResult(true, owner, "Router Lock disabled")
        }.getOrElse { LockResult(false, owner, it.message ?: "Unable to stop Router Lock") }
    }
}

data class LockResult(val success: Boolean, val deviceOwner: Boolean, val message: String)
