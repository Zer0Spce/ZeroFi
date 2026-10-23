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

    fun openHotspotSettings(context: Context) {
        launchSettings(
            context,
            Intent(ACTION_TETHER_SETTINGS),
            Intent(Settings.ACTION_WIRELESS_SETTINGS),
        )
    }

    fun openUsageAccessSettings(context: Context) {
        launchSettings(
            context,
            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            },
            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS),
        )
    }

    fun openDataUsageSettings(context: Context) {
        launchSettings(
            context,
            Intent(Settings.ACTION_DATA_USAGE_SETTINGS),
            Intent(Settings.ACTION_SETTINGS),
        )
    }

    fun openAppSettings(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
    }

    private fun launchSettings(context: Context, primary: Intent, fallback: Intent) {
        primary.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(primary) }
            .onFailure { runCatching { context.startActivity(fallback) } }
    }
}

object RouterLockController {
    private fun adminComponent(context: Context) =
        ComponentName(context, ZeroFiDeviceAdminReceiver::class.java)

    fun isDeviceOwner(context: Context): Boolean {
        val dpm = context.getSystemService(DevicePolicyManager::class.java)
        return dpm.isDeviceOwnerApp(context.packageName)
    }

    fun isLockTaskRunning(context: Context): Boolean {
        val activityManager = context.getSystemService(ActivityManager::class.java)
        return activityManager.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE
    }

    fun start(activity: Activity): LockResult {
        val dpm = activity.getSystemService(DevicePolicyManager::class.java)
        val owner = dpm.isDeviceOwnerApp(activity.packageName)
        return runCatching {
            if (owner) {
                dpm.setLockTaskPackages(adminComponent(activity), arrayOf(activity.packageName))
                // setLockTaskFeatures was added in Android 9 / API 28. Older
                // Android versions can still use Device Owner lock task mode;
                // they simply keep the platform's default lock-task features.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    dpm.setLockTaskFeatures(
                        adminComponent(activity),
                        DevicePolicyManager.LOCK_TASK_FEATURE_NONE,
                    )
                }
            }
            activity.startLockTask()
            LockResult(
                success = true,
                deviceOwner = owner,
                message = if (owner) {
                    "Dedicated Router Lock enabled"
                } else {
                    "Screen pinning requested"
                },
            )
        }.getOrElse {
            LockResult(false, owner, it.message ?: "Unable to start Router Lock")
        }
    }

    fun stop(activity: Activity): LockResult {
        val owner = isDeviceOwner(activity)
        return runCatching {
            activity.stopLockTask()
            LockResult(true, owner, "Router Lock disabled")
        }.getOrElse {
            LockResult(false, owner, it.message ?: "Unable to stop Router Lock")
        }
    }
}

data class LockResult(
    val success: Boolean,
    val deviceOwner: Boolean,
    val message: String,
)
