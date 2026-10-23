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
    val monitor = remember { SystemMonitor(context) }
    val tetheringWatcher = remember { TetheringWatcher(context) }

    var screen by remember { mutableStateOf(AppScreen.DASHBOARD) }
    var snapshot by remember { mutableStateOf(SystemSnapshot()) }
    var hotspotActive by remember { mutableStateOf<Boolean?>(null) }
    var detectedBand by remember { mutableStateOf<String?>(null) }
    var lockRunning by remember { mutableStateOf(RouterLockController.isLockTaskRunning(context)) }
    var settings by remember {
        mutableStateOf(
            ZeroFiSettingsState(
                accentTheme = preferences.accentTheme,
                darkMode = preferences.darkMode,
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

    DisposableEffect(Unit) {
        tetheringWatcher.start { active, band ->
            hotspotActive = active
            detectedBand = band
        }
        onDispose { tetheringWatcher.stop() }
    }

    LaunchedEffect(Unit) {
        if (settings.dedicatedBatterySaver && DedicatedModeController.isSupported(context)) {
            DedicatedModeController.setUserAppsSuspended(context, true)
        }
    }

    LaunchedEffect(settings.cycleResetDay) {
        while (true) {
            snapshot = monitor.snapshot(settings.cycleResetDay)
            lockRunning = RouterLockController.isLockTaskRunning(context)
            delay(3_000L)
        }
    }

    ZeroFiTheme(
        accentTheme = settings.accentTheme,
        darkMode = settings.darkMode,
    ) {
        when (screen) {
            AppScreen.DASHBOARD -> DashboardScreen(
                snapshot = snapshot,
                hotspotActive = hotspotActive,
                hotspotBand = detectedBand ?: settings.preferredBand,
                dataLimitGb = settings.dataLimitGb,
                accentTheme = settings.accentTheme,
                darkMode = settings.darkMode,
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
                    phonePermissionLauncher.launch(Manifest.permission.READ_PHONE_STATE)
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
                onBack = { screen = AppScreen.DASHBOARD },
                onKeepScreenOnChanged = { enabled ->
                    preferences.keepScreenOn = enabled
                    settings = settings.copy(keepScreenOn = enabled)
                    activity?.setKeepScreenOn(enabled)
                },
                onImmersiveModeChanged = { enabled ->
                    preferences.immersiveMode = enabled
                    settings = settings.copy(immersiveMode = enabled)
                    activity?.setImmersiveMode(enabled)
                },
                onBrightnessChanged = { value -> activity?.setAppBrightness(value) },
                onSystemBrightness = { activity?.setAppBrightness(null) },
            )

            AppScreen.SETTINGS -> SettingsScreen(
                settings = settings,
                deviceOwner = RouterLockController.isDeviceOwner(context),
                lockRunning = lockRunning,
                dedicatedModeSupported = DedicatedModeController.isSupported(context),
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
                        lockRunning = RouterLockController.isLockTaskRunning(context)
                        Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                    }
                },
                onDedicatedBatterySaverChanged = { enabled ->
                    val result = DedicatedModeController.setUserAppsSuspended(context, enabled)
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
