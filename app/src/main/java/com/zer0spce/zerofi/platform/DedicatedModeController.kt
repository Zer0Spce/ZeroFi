package com.zer0spce.zerofi.platform

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import com.zer0spce.zerofi.admin.ZeroFiDeviceAdminReceiver

/**
 * Optional battery-saving companion to Router Lock.
 *
 * On Android 7+ when ZeroFi is Device Owner, ordinary user-installed apps can be
 * suspended while ZeroFi is acting as a dedicated router. We intentionally do
 * not touch system apps, the launcher, phone, permission controller, Settings,
 * package installer/uninstaller, or any package Android refuses to suspend.
 */
object DedicatedModeController {
    private fun adminComponent(context: Context) =
        ComponentName(context, ZeroFiDeviceAdminReceiver::class.java)

    fun isSupported(context: Context): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
            context.getSystemService(DevicePolicyManager::class.java)
                .isDeviceOwnerApp(context.packageName)

    @Suppress("DEPRECATION")
    fun setUserAppsSuspended(context: Context, suspended: Boolean): DedicatedModeResult {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return DedicatedModeResult(
                success = false,
                changedPackages = 0,
                failedPackages = emptyList(),
                message = "App suspension requires Android 7 or newer",
            )
        }

        val dpm = context.getSystemService(DevicePolicyManager::class.java)
        if (!dpm.isDeviceOwnerApp(context.packageName)) {
            return DedicatedModeResult(
                success = false,
                changedPackages = 0,
                failedPackages = emptyList(),
                message = "Set ZeroFi as Device Owner to use Dedicated Battery Saver",
            )
        }

        val packageManager = context.packageManager
        val candidates = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            .asSequence()
            .filter { it.packageName != context.packageName }
            .filter { app ->
                val isSystem = app.flags and ApplicationInfo.FLAG_SYSTEM != 0
                val isUpdatedSystem = app.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0
                !isSystem && !isUpdatedSystem
            }
            .map { it.packageName }
            .distinct()
            .toList()

        if (candidates.isEmpty()) {
            return DedicatedModeResult(
                success = true,
                changedPackages = 0,
                failedPackages = emptyList(),
                message = if (suspended) "No extra user apps to suspend" else "No suspended user apps found",
            )
        }

        return runCatching {
            val failed = dpm.setPackagesSuspended(
                adminComponent(context),
                candidates.toTypedArray(),
                suspended,
            ).toList()
            val changed = candidates.size - failed.size
            DedicatedModeResult(
                success = true,
                changedPackages = changed,
                failedPackages = failed,
                message = if (suspended) {
                    "Dedicated Battery Saver enabled for $changed apps"
                } else {
                    "Restored $changed apps"
                },
            )
        }.getOrElse { error ->
            DedicatedModeResult(
                success = false,
                changedPackages = 0,
                failedPackages = emptyList(),
                message = error.message ?: "Unable to change app suspension state",
            )
        }
    }
}

data class DedicatedModeResult(
    val success: Boolean,
    val changedPackages: Int,
    val failedPackages: List<String>,
    val message: String,
)
