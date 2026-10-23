package com.zer0spce.zerofi.ui

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.zer0spce.zerofi.MainActivity
import com.zer0spce.zerofi.data.AppPreferences
import com.zer0spce.zerofi.platform.DedicatedModeController
import com.zer0spce.zerofi.platform.RouterController
import com.zer0spce.zerofi.platform.RouterLockController
import com.zer0spce.zerofi.platform.SystemMonitor
import com.zer0spce.zerofi.platform.SystemSnapshot
import com.zer0spce.zerofi.platform.TetheringWatcher
import com.zer0spce.zerofi.ui.theme.AccentTheme
import com.zer0spce.zerofi.ui.theme.ZeroFiTheme
import kotlinx.coroutines.delay


enum class AppScreen {
    DASHBOARD,
    HOTSPOT,
    DEVICES,
    DATA,
    DISPLAY,
    SETTINGS,
}

data class ZeroFiSettingsState(
    val accentTheme: AccentTheme,
    val darkMode: Boolean,
    val oledSaver: Boolean,
    val dataLimitGb: Float,
    val cycleResetDay: Int,
    val keepScreenOn: Boolean,
    val autoLaunchOnBoot: Boolean,
    val immersiveMode: Boolean,
    val dedicatedBatterySaver: Boolean,
    val hotspotSsid: String,
    val hotspotPassword: String,
    val hotspotSecurity: String,
    val preferredBand: String,
)

@Composable
fun ZeroFiApp() {
    val context = LocalContext.current
    val activity = context as? MainActivity
    val preferences = remember { AppPreferences(context) }
    val monitor = remember { runCatching { SystemMonitor(context) }.getOrNull() }
    val tetheringWatcher = remember { runCatching { TetheringWatcher(context) }.getOrNull() }

    var screen by remember { mutableStateOf(AppScreen.DASHBOARD) }
    var snapshot by remember { mutableStateOf(SystemSnapshot()) }
    var hotspotActive by remember { mutableStateOf<Boolean?>(null) }
    var detectedBand by remember { mutableStateOf<String?>(null) }
    var lockRunning by remember {
        mutableStateOf(runCatching { RouterLockController.isLockTaskRunning(context) }.getOrDefault(false))
    }
    var settings by remember {
        mutableStateOf(
            ZeroFiSettingsState(
                accentTheme = preferences.accentTheme,
                darkMode = preferences.darkMode,
                oledSaver = preferences.oledSaver,
                dataLimitGb = preferences.dataLimitGb,
                cycleResetDay = preferences.cycleResetDay,
                keepScreenOn = preferences.keepScreenOn,
                autoLaunchOnBoot = preferences.autoLaunchOnBoot,
                immersiveMode = preferences.immersiveMode,
                dedicatedBatterySaver = preferences.dedicatedBatterySaver,
                hotspotSsid = preferences.hotspotSsid,
                hotspotPassword = preferences.hotspotPassword,
                hotspotSecurity = preferences.hotspotSecurity,
                preferredBand = preferences.preferredBand,
            ),
        )
    }

    val phonePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        // The polling loop refreshes after returning.
    }

    DisposableEffect(tetheringWatcher) {
        runCatching {
            tetheringWatcher?.start { active, band ->
                hotspotActive = active
                detectedBand = band
            }
        }.onFailure {
            hotspotActive = null
            detectedBand = null
        }
        onDispose { runCatching { tetheringWatcher?.stop() } }
    }

    LaunchedEffect(Unit) {
        runCatching {
            if (settings.dedicatedBatterySaver && DedicatedModeController.isSupported(context)) {
                DedicatedModeController.setUserAppsSuspended(context, true)
            }
        }
    }

    LaunchedEffect(settings.cycleResetDay, monitor) {
        while (true) {
            if (monitor != null) {
                runCatching { monitor.snapshot(settings.cycleResetDay) }
                    .getOrNull()
                    ?.let { snapshot = it }
            }
            lockRunning = runCatching { RouterLockController.isLockTaskRunning(context) }
                .getOrDefault(false)
            delay(3_000L)
        }
    }

    ZeroFiTheme(
        accentTheme = settings.accentTheme,
        darkMode = settings.darkMode,
        oledSaver = settings.oledSaver,
    ) {
        when (screen) {
            AppScreen.DASHBOARD -> DashboardScreen(
                snapshot = snapshot,
                hotspotActive = hotspotActive,
                hotspotBand = detectedBand ?: settings.preferredBand,
                dataLimitGb = settings.dataLimitGb,
                accentTheme = settings.accentTheme,
                darkMode = settings.darkMode,
                oledSaver = settings.oledSaver,
                onAccentThemeChange = { theme ->
                    preferences.accentTheme = theme
                    settings = settings.copy(accentTheme = theme)
                },
                onDarkModeChange = { dark ->
                    preferences.darkMode = dark
                    settings = settings.copy(darkMode = dark)
                },
                onNavigate = { screen = it },
                onRequestPhonePermission = {
                    runCatching { phonePermissionLauncher.launch(Manifest.permission.READ_PHONE_STATE) }
                },
                onRequestUsageAccess = { RouterController.openUsageAccessSettings(context) },
            )

            AppScreen.HOTSPOT -> HotspotScreen(
                active = hotspotActive,
                detectedBand = detectedBand,
                ssid = settings.hotspotSsid,
                password = settings.hotspotPassword,
                security = settings.hotspotSecurity,
                preferredBand = settings.preferredBand,
                onBack = { screen = AppScreen.DASHBOARD },
                onOpenSystemHotspot = { RouterController.openHotspotSettings(context) },
                onSaveProfile = { ssid, password, security, band ->
                    preferences.hotspotSsid = ssid
                    preferences.hotspotPassword = password
                    preferences.hotspotSecurity = security
                    preferences.preferredBand = band
                    settings = settings.copy(
                        hotspotSsid = preferences.hotspotSsid,
                        hotspotPassword = preferences.hotspotPassword,
                        hotspotSecurity = preferences.hotspotSecurity,
                        preferredBand = preferences.preferredBand,
                    )
                    Toast.makeText(context, "Connection card saved", Toast.LENGTH_SHORT).show()
                },
            )

            AppScreen.DEVICES -> DevicesScreen(
                hotspotActive = hotspotActive,
                onBack = { screen = AppScreen.DASHBOARD },
                onOpenHotspotSettings = { RouterController.openHotspotSettings(context) },
            )

            AppScreen.DATA -> DataScreen(
                snapshot = snapshot,
                limitGb = settings.dataLimitGb,
                cycleDay = settings.cycleResetDay,
                onBack = { screen = AppScreen.DASHBOARD },
                onOpenUsageAccess = { RouterController.openUsageAccessSettings(context) },
                onOpenSystemDataUsage = { RouterController.openDataUsageSettings(context) },
                onLimitChanged = { value ->
                    preferences.dataLimitGb = value
                    settings = settings.copy(dataLimitGb = preferences.dataLimitGb)
                },
                onCycleDayChanged = { value ->
                    preferences.cycleResetDay = value
                    settings = settings.copy(cycleResetDay = preferences.cycleResetDay)
                },
            )

            AppScreen.DISPLAY -> DisplayScreen(
                keepScreenOn = settings.keepScreenOn,
                immersiveMode = settings.immersiveMode,
                oledSaver = settings.oledSaver,
                onBack = { screen = AppScreen.DASHBOARD },
                onKeepScreenOnChanged = { enabled ->
                    preferences.keepScreenOn = enabled
                    settings = settings.copy(keepScreenOn = enabled)
                    runCatching { activity?.setKeepScreenOn(enabled) }
                },
                onImmersiveModeChanged = { enabled ->
                    preferences.immersiveMode = enabled
                    settings = settings.copy(immersiveMode = enabled)
                    runCatching { activity?.setImmersiveMode(enabled) }
                },
                onOledSaverChanged = { enabled ->
                    preferences.oledSaver = enabled
                    preferences.darkMode = if (enabled) true else preferences.darkMode
                    settings = settings.copy(
                        oledSaver = enabled,
                        darkMode = if (enabled) true else settings.darkMode,
                    )
                },
                onBrightnessChanged = { value -> runCatching { activity?.setAppBrightness(value) } },
                onSystemBrightness = { runCatching { activity?.setAppBrightness(null) } },
            )

            AppScreen.SETTINGS -> SettingsScreen(
                settings = settings,
                deviceOwner = runCatching { RouterLockController.isDeviceOwner(context) }.getOrDefault(false),
                lockRunning = lockRunning,
                dedicatedModeSupported = runCatching { DedicatedModeController.isSupported(context) }.getOrDefault(false),
                onBack = { screen = AppScreen.DASHBOARD },
                onAutoLaunchChanged = { enabled ->
                    preferences.autoLaunchOnBoot = enabled
                    settings = settings.copy(autoLaunchOnBoot = enabled)
                },
                onRouterLockToggle = {
                    val host = activity
                    if (host == null) {
                        Toast.makeText(context, "Router Lock unavailable", Toast.LENGTH_SHORT).show()
                    } else {
                        val result = if (lockRunning) RouterLockController.stop(host) else RouterLockController.start(host)
                        lockRunning = runCatching { RouterLockController.isLockTaskRunning(context) }.getOrDefault(false)
                        Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                    }
                },
                onDedicatedBatterySaverChanged = { enabled ->
                    val result = runCatching {
                        DedicatedModeController.setUserAppsSuspended(context, enabled)
                    }.getOrElse {
                        com.zer0spce.zerofi.platform.DedicatedModeResult(
                            success = false,
                            changedPackages = 0,
                            failedPackages = emptyList(),
                            message = it.message ?: "Dedicated Battery Saver unavailable",
                        )
                    }
                    if (result.success) {
                        preferences.dedicatedBatterySaver = enabled
                        settings = settings.copy(dedicatedBatterySaver = enabled)
                    }
                    Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                },
                onOpenAppSettings = { RouterController.openAppSettings(context) },
            )
        }
    }
}
