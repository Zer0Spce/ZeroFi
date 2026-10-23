package com.zer0spce.zerofi.ui

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.zer0spce.zerofi.MainActivity
import com.zer0spce.zerofi.data.AppPreferences
import com.zer0spce.zerofi.platform.*
import com.zer0spce.zerofi.ui.theme.AccentTheme
import com.zer0spce.zerofi.ui.theme.ZeroFiTheme
import kotlinx.coroutines.delay

enum class AppScreen { DASHBOARD, HOTSPOT, DEVICES, DATA, DISPLAY, SETTINGS }

data class ZeroFiSettingsState(
    val accentTheme: AccentTheme,
    val darkMode: Boolean,
    val oledSaver: Boolean,
    val dataLimitGb: Float,
    val cycleResetDay: Int,
    val keepScreenOn: Boolean,
    val autoLaunchOnBoot: Boolean,
    val hotspotAutomationEnabled: Boolean,
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
    var lockRunning by remember { mutableStateOf(runCatching { RouterLockController.isLockTaskRunning(context) }.getOrDefault(false)) }
    var settings by remember {
        mutableStateOf(ZeroFiSettingsState(
            preferences.accentTheme, preferences.darkMode, preferences.oledSaver,
            preferences.dataLimitGb, preferences.cycleResetDay, preferences.keepScreenOn,
            preferences.autoLaunchOnBoot, preferences.hotspotAutomationEnabled,
            preferences.immersiveMode, preferences.dedicatedBatterySaver,
            preferences.hotspotSsid, preferences.hotspotPassword, preferences.hotspotSecurity,
            preferences.preferredBand,
        ))
    }

    val phonePermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    DisposableEffect(tetheringWatcher) {
        runCatching { tetheringWatcher?.start { active, band -> hotspotActive = active; detectedBand = band } }
            .onFailure { hotspotActive = null; detectedBand = null }
        onDispose { runCatching { tetheringWatcher?.stop() } }
    }

    LaunchedEffect(Unit) {
        if (settings.dedicatedBatterySaver && DedicatedModeController.isSupported(context)) {
            runCatching { DedicatedModeController.setUserAppsSuspended(context, true) }
        }
    }

    // Automation runs only after the user explicitly enables it in Settings.
    LaunchedEffect(settings.hotspotAutomationEnabled) {
        if (settings.hotspotAutomationEnabled && HotspotAutomationService.isEnabled(context)) {
            HotspotAutomationService.requestEnsureHotspotOn()
        }
    }

    LaunchedEffect(settings.cycleResetDay, monitor) {
        while (true) {
            monitor?.let { runCatching { it.snapshot(settings.cycleResetDay) }.getOrNull()?.let { s -> snapshot = s } }
            lockRunning = runCatching { RouterLockController.isLockTaskRunning(context) }.getOrDefault(false)
            delay(3_000L)
        }
    }

    fun toggleRouterLock() {
        val host = activity
        if (host == null) {
            Toast.makeText(context, "Router Lock unavailable", Toast.LENGTH_SHORT).show()
            return
        }
        val result = if (lockRunning) RouterLockController.stop(host) else RouterLockController.start(host)
        lockRunning = runCatching { RouterLockController.isLockTaskRunning(context) }.getOrDefault(false)
        Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
    }

    ZeroFiTheme(settings.accentTheme, settings.darkMode, settings.oledSaver) {
        when (screen) {
            AppScreen.DASHBOARD -> DashboardScreen(
                snapshot, hotspotActive, detectedBand ?: settings.preferredBand, settings.dataLimitGb,
                settings.accentTheme, settings.darkMode, settings.oledSaver,
                onAccentThemeChange = { preferences.accentTheme = it; settings = settings.copy(accentTheme = it) },
                onDarkModeChange = { preferences.darkMode = it; settings = settings.copy(darkMode = it) },
                onNavigate = { screen = it },
                onRequestPhonePermission = { runCatching { phonePermissionLauncher.launch(Manifest.permission.READ_PHONE_STATE) } },
                onRequestUsageAccess = { RouterController.openUsageAccessSettings(context) },
                lockRunning = lockRunning,
                onRouterLockToggle = ::toggleRouterLock,
            )
            AppScreen.HOTSPOT -> HotspotScreen(hotspotActive, detectedBand, settings.hotspotSsid, settings.hotspotPassword, settings.hotspotSecurity, settings.preferredBand,
                { screen = AppScreen.DASHBOARD }, { RouterController.openHotspotSettings(context) }) { ssid, password, security, band ->
                preferences.hotspotSsid = ssid; preferences.hotspotPassword = password; preferences.hotspotSecurity = security; preferences.preferredBand = band
                settings = settings.copy(hotspotSsid = preferences.hotspotSsid, hotspotPassword = preferences.hotspotPassword, hotspotSecurity = preferences.hotspotSecurity, preferredBand = preferences.preferredBand)
                Toast.makeText(context, "Connection card saved", Toast.LENGTH_SHORT).show()
            }
            AppScreen.DEVICES -> DevicesScreen(hotspotActive, { screen = AppScreen.DASHBOARD }) { RouterController.openHotspotSettings(context) }
            AppScreen.DATA -> DataScreen(snapshot, settings.dataLimitGb, settings.cycleResetDay, { screen = AppScreen.DASHBOARD },
                { RouterController.openUsageAccessSettings(context) }, { RouterController.openDataUsageSettings(context) },
                { preferences.dataLimitGb = it; settings = settings.copy(dataLimitGb = preferences.dataLimitGb) },
                { preferences.cycleResetDay = it; settings = settings.copy(cycleResetDay = preferences.cycleResetDay) })
            AppScreen.DISPLAY -> DisplayScreen(settings.keepScreenOn, settings.immersiveMode, settings.oledSaver, { screen = AppScreen.DASHBOARD },
                { preferences.keepScreenOn = it; settings = settings.copy(keepScreenOn = it); activity?.setKeepScreenOn(it) },
                { preferences.immersiveMode = it; settings = settings.copy(immersiveMode = it); activity?.setImmersiveMode(it) },
                { preferences.oledSaver = it; if (it) preferences.darkMode = true; settings = settings.copy(oledSaver = it, darkMode = if (it) true else settings.darkMode) },
                { activity?.setAppBrightness(it) }, { activity?.setAppBrightness(null) })
            AppScreen.SETTINGS -> SettingsScreen(
                settings, RouterLockController.isDeviceOwner(context), lockRunning, DedicatedModeController.isSupported(context),
                { screen = AppScreen.DASHBOARD },
                { preferences.autoLaunchOnBoot = it; settings = settings.copy(autoLaunchOnBoot = it) },
                ::toggleRouterLock,
                { enabled ->
                    val result = runCatching { DedicatedModeController.setUserAppsSuspended(context, enabled) }.getOrNull()
                    if (result?.success == true) { preferences.dedicatedBatterySaver = enabled; settings = settings.copy(dedicatedBatterySaver = enabled) }
                    Toast.makeText(context, result?.message ?: "Dedicated Battery Saver unavailable", Toast.LENGTH_LONG).show()
                },
                { RouterController.openAppSettings(context) },
                hotspotAutomationEnabled = settings.hotspotAutomationEnabled,
                hotspotAutomationConfigured = HotspotAutomationService.isEnabled(context),
                onHotspotAutomationChanged = { enabled ->
                    preferences.hotspotAutomationEnabled = enabled
                    settings = settings.copy(hotspotAutomationEnabled = enabled)
                    if (enabled && !HotspotAutomationService.isEnabled(context)) {
                        HotspotLaunchController.resetSetupPrompt(activity ?: return@SettingsScreen)
                        HotspotLaunchController.ensureConfiguredOrRun(activity)
                    } else if (enabled) HotspotAutomationService.requestEnsureHotspotOn()
                },
            )
        }
    }
}
