package com.zer0spce.zerofi.ui

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.zer0spce.zerofi.MainActivity
import com.zer0spce.zerofi.data.AppPreferences
import com.zer0spce.zerofi.platform.*
import com.zer0spce.zerofi.ui.theme.AccentTheme
import com.zer0spce.zerofi.ui.theme.ZeroFiTheme
import kotlinx.coroutines.delay

enum class AppScreen { DASHBOARD, HOTSPOT, DEVICES, DATA, DISPLAY, SETTINGS, DIAGNOSTICS, STANDBY }

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
    val hotspotProfileConfirmed: Boolean,
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
    var liveHotspotSsid by remember { mutableStateOf<String?>(null) }
    var liveHotspotSecurity by remember { mutableStateOf<String?>(null) }
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
                hotspotAutomationEnabled = preferences.hotspotAutomationEnabled,
                immersiveMode = preferences.immersiveMode,
                dedicatedBatterySaver = preferences.dedicatedBatterySaver,
                hotspotSsid = preferences.hotspotSsid,
                hotspotPassword = preferences.hotspotPassword,
                hotspotSecurity = preferences.hotspotSecurity,
                hotspotProfileConfirmed = preferences.hotspotProfileConfirmed,
                preferredBand = preferences.preferredBand,
            )
        )
    }

    val phonePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    DisposableEffect(tetheringWatcher) {
        runCatching {
            tetheringWatcher?.start { status ->
                hotspotActive = status.active
                detectedBand = status.band
                liveHotspotSsid = status.ssid
                liveHotspotSecurity = status.security

                val liveSsid = status.ssid?.trim().orEmpty()
                if (liveSsid.isNotBlank()) {
                    val desiredSsid = preferences.hotspotSsid.trim()
                    val openNetwork = status.security.equals("nopass", true)
                    val exposedPassword = status.password

                    if (!exposedPassword.isNullOrBlank()) {
                        preferences.hotspotSsid = liveSsid
                        preferences.hotspotPassword = exposedPassword
                        status.security?.let { preferences.hotspotSecurity = it }
                        preferences.hotspotProfileConfirmed = true
                    } else if (liveSsid == desiredSsid) {
                        status.security?.let { preferences.hotspotSecurity = it }
                        val savedPasswordLooksValid = preferences.hotspotPassword.length in 8..63
                        preferences.hotspotProfileConfirmed = openNetwork || savedPasswordLooksValid
                    } else {
                        preferences.hotspotProfileConfirmed = false
                    }

                    settings = settings.copy(
                        hotspotSsid = preferences.hotspotSsid,
                        hotspotPassword = preferences.hotspotPassword,
                        hotspotSecurity = preferences.hotspotSecurity,
                        hotspotProfileConfirmed = preferences.hotspotProfileConfirmed,
                    )
                }
            }
        }
        onDispose { runCatching { tetheringWatcher?.stop() } }
    }

    LaunchedEffect(Unit) {
        if (settings.dedicatedBatterySaver && DedicatedModeController.isSupported(context)) {
            runCatching { DedicatedModeController.setUserAppsSuspended(context, true) }
        }
    }

    LaunchedEffect(settings.hotspotAutomationEnabled) {
        if (settings.hotspotAutomationEnabled && HotspotAutomationService.isEnabled(context)) {
            HotspotAutomationService.requestEnsureHotspotOn()
        }
    }

    LaunchedEffect(settings.cycleResetDay, monitor) {
        while (true) {
            monitor?.let {
                runCatching { it.snapshot(settings.cycleResetDay) }
                    .getOrNull()
                    ?.let { s -> snapshot = s }
            }
            lockRunning = runCatching { RouterLockController.isLockTaskRunning(context) }.getOrDefault(false)
            delay(2_000L)
        }
    }

    fun toggleLock() {
        val host = activity ?: return
        val result = if (lockRunning) RouterLockController.stop(host) else RouterLockController.start(host)
        lockRunning = runCatching { RouterLockController.isLockTaskRunning(context) }.getOrDefault(false)
        Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
    }

    fun applyHotspotProfile(ssid: String, password: String, security: String, band: String) {
        val cleanSsid = ssid.trim().take(32)
        val openNetwork = security.equals("nopass", true)
        if (cleanSsid.isBlank()) {
            Toast.makeText(context, "Enter a hotspot name first", Toast.LENGTH_SHORT).show()
            return
        }
        if (!openNetwork && password.length !in 8..63) {
            Toast.makeText(context, "Hotspot password must be 8–63 characters", Toast.LENGTH_LONG).show()
            return
        }

        preferences.hotspotSsid = cleanSsid
        preferences.hotspotPassword = if (openNetwork) "" else password
        preferences.hotspotSecurity = security
        preferences.preferredBand = band
        preferences.hotspotProfileConfirmed = false
        settings = settings.copy(
            hotspotSsid = preferences.hotspotSsid,
            hotspotPassword = preferences.hotspotPassword,
            hotspotSecurity = preferences.hotspotSecurity,
            preferredBand = preferences.preferredBand,
            hotspotProfileConfirmed = false,
        )

        val host = activity
        if (!HotspotAutomationService.isEnabled(context)) {
            if (host != null) {
                HotspotLaunchController.resetSetupPrompt(host)
                HotspotLaunchController.ensureConfiguredOrRun(host)
                Toast.makeText(
                    context,
                    "Enable ZeroFi Hotspot Automation, return here, then tap Apply again.",
                    Toast.LENGTH_LONG,
                ).show()
            } else {
                RouterController.openHotspotSettings(context)
            }
            return
        }

        val started = HotspotAutomationService.requestApplyHotspotProfile(
            preferences.hotspotSsid,
            preferences.hotspotPassword,
            openNetwork,
        )
        if (started) {
            Toast.makeText(
                context,
                "Applying the real Android hotspot name and password…",
                Toast.LENGTH_LONG,
            ).show()
        } else {
            RouterController.openHotspotSettings(context)
            Toast.makeText(
                context,
                "Could not automate this device. Update the hotspot values in Android Settings.",
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    ZeroFiTheme(settings.accentTheme, settings.darkMode, settings.oledSaver) {
        Box(Modifier.fillMaxSize()) {
            when (screen) {
                AppScreen.DASHBOARD -> DashboardScreen(
                    snapshot = snapshot,
                    hotspotActive = hotspotActive,
                    hotspotBand = detectedBand ?: settings.preferredBand,
                    dataLimitGb = settings.dataLimitGb,
                    accentTheme = settings.accentTheme,
                    darkMode = settings.darkMode,
                    oledSaver = settings.oledSaver,
                    onAccentThemeChange = {
                        preferences.accentTheme = it
                        settings = settings.copy(accentTheme = it)
                    },
                    onDarkModeChange = {
                        preferences.darkMode = it
                        settings = settings.copy(darkMode = it)
                    },
                    onNavigate = { screen = it },
                    onRequestPhonePermission = {
                        runCatching { phonePermissionLauncher.launch(Manifest.permission.READ_PHONE_STATE) }
                    },
                    onRequestUsageAccess = { RouterController.openUsageAccessSettings(context) },
                    lockRunning = lockRunning,
                    onRouterLockToggle = ::toggleLock,
                )

                AppScreen.HOTSPOT -> HotspotScreen(
                    active = hotspotActive,
                    detectedBand = detectedBand,
                    liveSsid = liveHotspotSsid,
                    liveSecurity = liveHotspotSecurity,
                    ssid = settings.hotspotSsid,
                    password = settings.hotspotPassword,
                    security = settings.hotspotSecurity,
                    preferredBand = settings.preferredBand,
                    profileConfirmed = settings.hotspotProfileConfirmed,
                    automationReady = HotspotAutomationService.isEnabled(context),
                    onBack = { screen = AppScreen.DASHBOARD },
                    onOpenSystemHotspot = { RouterController.openHotspotSettings(context) },
                    onApplyProfile = ::applyHotspotProfile,
                )

                AppScreen.DEVICES -> DevicesScreen(
                    hotspotActive,
                    { screen = AppScreen.DASHBOARD },
                ) { RouterController.openHotspotSettings(context) }

                AppScreen.DATA -> DataScreen(
                    snapshot,
                    settings.dataLimitGb,
                    settings.cycleResetDay,
                    { screen = AppScreen.DASHBOARD },
                    { RouterController.openUsageAccessSettings(context) },
                    { RouterController.openDataUsageSettings(context) },
                    {
                        preferences.dataLimitGb = it
                        settings = settings.copy(dataLimitGb = preferences.dataLimitGb)
                    },
                    {
                        preferences.cycleResetDay = it
                        settings = settings.copy(cycleResetDay = preferences.cycleResetDay)
                    },
                )

                AppScreen.DISPLAY -> DisplayScreen(
                    settings.keepScreenOn,
                    settings.immersiveMode,
                    settings.oledSaver,
                    { screen = AppScreen.DASHBOARD },
                    {
                        preferences.keepScreenOn = it
                        settings = settings.copy(keepScreenOn = it)
                        activity?.setKeepScreenOn(it)
                    },
                    {
                        preferences.immersiveMode = it
                        settings = settings.copy(immersiveMode = it)
                        activity?.setImmersiveMode(it)
                    },
                    {
                        preferences.oledSaver = it
                        settings = settings.copy(oledSaver = it)
                    },
                    { activity?.setAppBrightness(it) },
                    { activity?.setAppBrightness(null) },
                )

                AppScreen.SETTINGS -> SettingsScreen(
                    settings,
                    RouterLockController.isDeviceOwner(context),
                    lockRunning,
                    DedicatedModeController.isSupported(context),
                    { screen = AppScreen.DASHBOARD },
                    {
                        preferences.autoLaunchOnBoot = it
                        settings = settings.copy(autoLaunchOnBoot = it)
                    },
                    ::toggleLock,
                    { enabled ->
                        val r = runCatching {
                            DedicatedModeController.setUserAppsSuspended(context, enabled)
                        }.getOrNull()
                        if (r?.success == true) {
                            preferences.dedicatedBatterySaver = enabled
                            settings = settings.copy(dedicatedBatterySaver = enabled)
                        }
                    },
                    { RouterController.openAppSettings(context) },
                    { screen = AppScreen.DIAGNOSTICS },
                    { screen = AppScreen.STANDBY },
                )

                AppScreen.DIAGNOSTICS -> DiagnosticsScreen(
                    snapshot,
                    hotspotActive,
                    detectedBand ?: settings.preferredBand,
                ) { screen = AppScreen.SETTINGS }

                AppScreen.STANDBY -> StandbyScreen(snapshot, hotspotActive) {
                    screen = AppScreen.DASHBOARD
                }
            }

            if (screen == AppScreen.SETTINGS) {
                HotspotAutomationSettingsButton(
                    enabled = settings.hotspotAutomationEnabled,
                    configured = HotspotAutomationService.isEnabled(context),
                    onClick = {
                        val host = activity ?: return@HotspotAutomationSettingsButton
                        if (!settings.hotspotAutomationEnabled || !HotspotAutomationService.isEnabled(context)) {
                            preferences.hotspotAutomationEnabled = true
                            settings = settings.copy(hotspotAutomationEnabled = true)
                            HotspotLaunchController.resetSetupPrompt(host)
                            HotspotLaunchController.ensureConfiguredOrRun(host)
                        } else {
                            preferences.hotspotAutomationEnabled = false
                            settings = settings.copy(hotspotAutomationEnabled = false)
                            Toast.makeText(context, "Hotspot automation disabled", Toast.LENGTH_SHORT).show()
                        }
                    },
                )
            }
        }
    }
}
