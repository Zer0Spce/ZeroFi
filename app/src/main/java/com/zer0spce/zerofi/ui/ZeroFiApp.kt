package com.zer0spce.zerofi.ui

import android.Manifest
import android.os.Build
import android.os.SystemClock
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

enum class AppScreen {
    DASHBOARD, HOTSPOT, DEVICES, DATA, DISPLAY, SETTINGS, DIAGNOSTICS, STANDBY,
    CONTROL_CENTER, SIGNAL_HISTORY, SPEED_TEST, NETWORK_TOOLS, BACKUP_RESTORE,
}

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
    val autoOffMinutes: Int,
    val keepHotspotWhileCharging: Boolean,
    val dataAlertsEnabled: Boolean,
    val thermalAlertsEnabled: Boolean,
    val thermalThresholdC: Float,
    val restoreRouterLock: Boolean,
    val autoOledOnStandby: Boolean,
)

private fun readSettings(p: AppPreferences) = ZeroFiSettingsState(
    accentTheme = p.accentTheme,
    darkMode = p.darkMode,
    oledSaver = p.oledSaver,
    dataLimitGb = p.dataLimitGb,
    cycleResetDay = p.cycleResetDay,
    keepScreenOn = p.keepScreenOn,
    autoLaunchOnBoot = p.autoLaunchOnBoot,
    hotspotAutomationEnabled = p.hotspotAutomationEnabled,
    immersiveMode = p.immersiveMode,
    dedicatedBatterySaver = p.dedicatedBatterySaver,
    hotspotSsid = p.hotspotSsid,
    hotspotPassword = p.hotspotPassword,
    hotspotSecurity = p.hotspotSecurity,
    hotspotProfileConfirmed = p.hotspotProfileConfirmed,
    preferredBand = p.preferredBand,
    autoOffMinutes = p.autoOffMinutes,
    keepHotspotWhileCharging = p.keepHotspotWhileCharging,
    dataAlertsEnabled = p.dataAlertsEnabled,
    thermalAlertsEnabled = p.thermalAlertsEnabled,
    thermalThresholdC = p.thermalThresholdC,
    restoreRouterLock = p.restoreRouterLock,
    autoOledOnStandby = p.autoOledOnStandby,
)

@Composable
fun ZeroFiApp() {
    val context = LocalContext.current
    val activity = context as? MainActivity
    val preferences = remember { AppPreferences(context) }
    val monitor = remember { runCatching { SystemMonitor(context) }.getOrNull() }
    val featureMonitor = remember { RouterFeatureMonitor(context) }
    val tetheringWatcher = remember { runCatching { TetheringWatcher(context) }.getOrNull() }

    var screen by remember { mutableStateOf(AppScreen.DASHBOARD) }
    var snapshot by remember { mutableStateOf(SystemSnapshot()) }
    var featureSnapshot by remember { mutableStateOf(RouterFeatureSnapshot()) }
    var clientSnapshot by remember { mutableStateOf(HotspotClientSnapshot()) }
    val signalHistory = remember { mutableStateListOf<SignalSample>() }
    var hotspotActive by remember { mutableStateOf<Boolean?>(null) }
    var detectedBand by remember { mutableStateOf<String?>(null) }
    var liveHotspotSsid by remember { mutableStateOf<String?>(null) }
    var liveHotspotSecurity by remember { mutableStateOf<String?>(null) }
    var lockRunning by remember { mutableStateOf(runCatching { RouterLockController.isLockTaskRunning(context) }.getOrDefault(false)) }
    var settings by remember { mutableStateOf(readSettings(preferences)) }
    var autoOffIdleSince by remember { mutableStateOf<Long?>(null) }
    var autoOffIssued by remember { mutableStateOf(false) }
    var thermalAlerted by remember { mutableStateOf(false) }

    val phonePermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

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
                        preferences.hotspotProfileConfirmed = openNetwork || preferences.hotspotPassword.length in 8..63
                    } else preferences.hotspotProfileConfirmed = false
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
        delay(800L)
        if (settings.restoreRouterLock && activity != null && !RouterLockController.isLockTaskRunning(context)) {
            val result = RouterLockController.start(activity)
            lockRunning = RouterLockController.isLockTaskRunning(context)
            if (!result.success) Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(settings.hotspotAutomationEnabled) {
        if (settings.hotspotAutomationEnabled && HotspotAutomationService.isEnabled(context)) {
            HotspotAutomationService.requestEnsureHotspotOn()
        }
    }

    LaunchedEffect(settings.cycleResetDay, monitor) {
        while (true) {
            monitor?.let { runCatching { it.snapshot(settings.cycleResetDay) }.getOrNull()?.let { s -> snapshot = s } }
            lockRunning = runCatching { RouterLockController.isLockTaskRunning(context) }.getOrDefault(false)
            delay(2_000L)
        }
    }

    LaunchedEffect(featureMonitor) {
        while (true) {
            featureSnapshot = runCatching { featureMonitor.snapshot() }.getOrDefault(featureSnapshot)
            clientSnapshot = withContext(Dispatchers.IO) { HotspotClientScanner.scan() }
            delay(5_000L)
        }
    }

    LaunchedEffect(snapshot.signalDbm) {
        val dbm = snapshot.signalDbm ?: return@LaunchedEffect
        signalHistory.add(SignalSample(System.currentTimeMillis(), dbm))
        while (signalHistory.size > 1_800) signalHistory.removeAt(0)
    }

    LaunchedEffect(snapshot.monthBytes, settings.dataLimitGb, settings.dataAlertsEnabled) {
        if (!settings.dataAlertsEnabled) return@LaunchedEffect
        val used = snapshot.monthBytes ?: return@LaunchedEffect
        val limit = (settings.dataLimitGb * 1024 * 1024 * 1024L).toLong().coerceAtLeast(1)
        val percent = (used * 100.0 / limit).toInt()
        if (percent < 40 && preferences.lastDataAlertLevel > 0) preferences.lastDataAlertLevel = 0
        val level = when {
            percent >= 100 -> 100
            percent >= 90 -> 90
            percent >= 75 -> 75
            percent >= 50 -> 50
            else -> 0
        }
        if (level > preferences.lastDataAlertLevel) {
            RouterAlertNotifier.notifyDataUsage(context, level, formatBytes(used), "${settings.dataLimitGb.roundToInt()} GB")
            preferences.lastDataAlertLevel = level
        }
    }

    LaunchedEffect(snapshot.batteryTempC, settings.thermalAlertsEnabled, settings.thermalThresholdC) {
        val temp = snapshot.batteryTempC ?: return@LaunchedEffect
        if (settings.thermalAlertsEnabled && temp >= settings.thermalThresholdC && !thermalAlerted) {
            RouterAlertNotifier.notifyThermal(context, temp)
            thermalAlerted = true
        } else if (temp <= settings.thermalThresholdC - 2f) thermalAlerted = false
    }

    LaunchedEffect(hotspotActive, clientSnapshot, settings.autoOffMinutes, settings.keepHotspotWhileCharging, snapshot.charging) {
        if (hotspotActive != true || settings.autoOffMinutes <= 0 || !clientSnapshot.available || clientSnapshot.clients.isNotEmpty() || (settings.keepHotspotWhileCharging && snapshot.charging)) {
            autoOffIdleSince = null
            if (hotspotActive != true || clientSnapshot.clients.isNotEmpty()) autoOffIssued = false
            return@LaunchedEffect
        }
        if (autoOffIdleSince == null) autoOffIdleSince = SystemClock.elapsedRealtime()
        while (hotspotActive == true && !autoOffIssued) {
            val started = autoOffIdleSince ?: break
            if (SystemClock.elapsedRealtime() - started >= settings.autoOffMinutes * 60_000L) {
                if (HotspotAutomationService.requestEnsureHotspotOff()) {
                    autoOffIssued = true
                    RouterAlertNotifier.notifyAutoOff(context, settings.autoOffMinutes)
                }
                break
            }
            delay(5_000L)
        }
    }

    fun toggleLock() {
        val host = activity ?: return
        val result = if (lockRunning) RouterLockController.stop(host) else RouterLockController.start(host)
        lockRunning = runCatching { RouterLockController.isLockTaskRunning(context) }.getOrDefault(false)
        Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
    }

    fun toggleHotspotAutomation() {
        val host = activity ?: return
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
    }

    fun toggleHotspotPower() {
        if (!HotspotAutomationService.isEnabled(context)) {
            RouterController.openHotspotSettings(context)
            Toast.makeText(context, "Enable Hotspot Automation for one-tap hotspot control.", Toast.LENGTH_LONG).show()
            return
        }
        if (hotspotActive == true) HotspotAutomationService.requestEnsureHotspotOff() else HotspotAutomationService.requestEnsureHotspotOn()
    }

    fun applyHotspotProfile(ssid: String, password: String, security: String, band: String) {
        val cleanSsid = ssid.trim().take(32)
        val openNetwork = security.equals("nopass", true)
        if (cleanSsid.isBlank()) { Toast.makeText(context, "Enter a hotspot name first", Toast.LENGTH_SHORT).show(); return }
        if (!openNetwork && password.length !in 8..63) { Toast.makeText(context, "Hotspot password must be 8–63 characters", Toast.LENGTH_LONG).show(); return }
        preferences.hotspotSsid = cleanSsid
        preferences.hotspotPassword = if (openNetwork) "" else password
        preferences.hotspotSecurity = security
        preferences.preferredBand = band
        preferences.hotspotProfileConfirmed = false
        settings = readSettings(preferences)
        if (!HotspotAutomationService.isEnabled(context)) {
            val host = activity
            if (host != null) {
                HotspotLaunchController.resetSetupPrompt(host)
                HotspotLaunchController.ensureConfiguredOrRun(host)
                Toast.makeText(context, "Enable ZeroFi Hotspot Automation, return here, then tap Apply again.", Toast.LENGTH_LONG).show()
            } else RouterController.openHotspotSettings(context)
            return
        }
        val started = HotspotAutomationService.requestApplyHotspotProfile(preferences.hotspotSsid, preferences.hotspotPassword, openNetwork) { success, message ->
            preferences.hotspotProfileConfirmed = success
            settings = settings.copy(hotspotProfileConfirmed = success)
            Toast.makeText(context, if (success) "$message. QR code is ready." else message, Toast.LENGTH_LONG).show()
        }
        if (started) Toast.makeText(context, "Opening Wi-Fi hotspot details and applying the profile…", Toast.LENGTH_LONG).show()
        else { RouterController.openHotspotSettings(context); Toast.makeText(context, "Could not automate this device. Update hotspot values in Android Settings.", Toast.LENGTH_LONG).show() }
    }

    ZeroFiTheme(settings.accentTheme, settings.darkMode, settings.oledSaver) {
        Box(Modifier.fillMaxSize()) {
            when (screen) {
                AppScreen.DASHBOARD -> DashboardScreenV2(
                    snapshot, hotspotActive, detectedBand ?: settings.preferredBand, settings.dataLimitGb, settings.accentTheme, settings.darkMode, settings.oledSaver,
                    { preferences.accentTheme = it; settings = settings.copy(accentTheme = it) },
                    { preferences.darkMode = it; settings = settings.copy(darkMode = it) },
                    { screen = it },
                    { runCatching { phonePermissionLauncher.launch(Manifest.permission.READ_PHONE_STATE) } },
                    { RouterController.openUsageAccessSettings(context) },
                    lockRunning, ::toggleLock,
                )

                AppScreen.HOTSPOT -> HotspotScreenV2(
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
                    onToggleHotspot = ::toggleHotspotPower,
                    onOpenSystemHotspot = { RouterController.openHotspotSettings(context) },
                    onApplyProfile = ::applyHotspotProfile,
                )

                AppScreen.DEVICES -> ClientManagementScreen(
                    clientSnapshot,
                    preferences.trustedClientMacs,
                    { preferences.clientAlias(it) },
                    { mac ->
                        val updated = preferences.trustedClientMacs.toMutableSet()
                        if (!updated.add(mac.lowercase())) updated.remove(mac.lowercase())
                        preferences.trustedClientMacs = updated
                    },
                    { mac, alias -> preferences.setClientAlias(mac, alias) },
                    { RouterController.openHotspotSettings(context) },
                    { screen = AppScreen.DASHBOARD },
                )

                AppScreen.DATA -> DataUsageV2Screen(
                    snapshot, featureSnapshot, settings, RouterAlertNotifier.notificationsGranted(context),
                    { screen = AppScreen.DASHBOARD },
                    { preferences.dataLimitGb = it; settings = settings.copy(dataLimitGb = preferences.dataLimitGb) },
                    { preferences.cycleResetDay = it; settings = settings.copy(cycleResetDay = preferences.cycleResetDay) },
                    { preferences.dataAlertsEnabled = it; settings = settings.copy(dataAlertsEnabled = it) },
                    { if (Build.VERSION.SDK_INT >= 33) notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                    { RouterController.openDataUsageSettings(context) },
                    { RouterController.openUsageAccessSettings(context) },
                )

                AppScreen.DISPLAY -> DisplayScreen(
                    settings.keepScreenOn, settings.immersiveMode, settings.oledSaver, { screen = AppScreen.DASHBOARD },
                    { preferences.keepScreenOn = it; settings = settings.copy(keepScreenOn = it); activity?.setKeepScreenOn(it) },
                    { preferences.immersiveMode = it; settings = settings.copy(immersiveMode = it); activity?.setImmersiveMode(it) },
                    { preferences.oledSaver = it; settings = settings.copy(oledSaver = it) },
                    { activity?.setAppBrightness(it) }, { activity?.setAppBrightness(null) },
                )

                AppScreen.SETTINGS -> RouterSettingsV2(
                    settings,
                    RouterLockController.isDeviceOwner(context),
                    lockRunning,
                    DedicatedModeController.isSupported(context),
                    HotspotAutomationService.isEnabled(context),
                    RouterAlertNotifier.notificationsGranted(context),
                    { screen = AppScreen.DASHBOARD },
                    { preferences.autoLaunchOnBoot = it; settings = settings.copy(autoLaunchOnBoot = it) },
                    { preferences.restoreRouterLock = it; settings = settings.copy(restoreRouterLock = it) },
                    ::toggleLock,
                    { enabled ->
                        val result = runCatching { DedicatedModeController.setUserAppsSuspended(context, enabled) }.getOrNull()
                        if (result?.success == true) { preferences.dedicatedBatterySaver = enabled; settings = settings.copy(dedicatedBatterySaver = enabled) }
                    },
                    { screen = AppScreen.DISPLAY },
                    { preferences.oledSaver = it; settings = settings.copy(oledSaver = it) },
                    { preferences.autoOledOnStandby = it; settings = settings.copy(autoOledOnStandby = it) },
                    ::toggleHotspotAutomation,
                    { preferences.autoOffMinutes = it; settings = settings.copy(autoOffMinutes = it) },
                    { preferences.keepHotspotWhileCharging = it; settings = settings.copy(keepHotspotWhileCharging = it) },
                    { preferences.dataAlertsEnabled = it; settings = settings.copy(dataAlertsEnabled = it) },
                    { preferences.thermalAlertsEnabled = it; settings = settings.copy(thermalAlertsEnabled = it) },
                    { preferences.thermalThresholdC = it; settings = settings.copy(thermalThresholdC = it) },
                    { if (Build.VERSION.SDK_INT >= 33) notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                    { RouterController.openAppSettings(context) },
                    { screen = AppScreen.CONTROL_CENTER },
                    { screen = AppScreen.SIGNAL_HISTORY },
                    { screen = AppScreen.SPEED_TEST },
                    { screen = AppScreen.NETWORK_TOOLS },
                    { screen = AppScreen.BACKUP_RESTORE },
                    { screen = AppScreen.DIAGNOSTICS },
                    { screen = AppScreen.STANDBY },
                )

                AppScreen.CONTROL_CENTER -> RouterControlCenterScreen(
                    snapshot, featureSnapshot, hotspotActive, detectedBand ?: settings.preferredBand,
                    clientSnapshot, settings, { screen = AppScreen.DASHBOARD }, { screen = it }, ::toggleHotspotPower,
                )

                AppScreen.SIGNAL_HISTORY -> SignalHistoryScreen(signalHistory, { screen = AppScreen.CONTROL_CENTER })
                AppScreen.SPEED_TEST -> SpeedTestScreen { screen = AppScreen.CONTROL_CENTER }
                AppScreen.NETWORK_TOOLS -> NetworkToolsScreen(
                    featureSnapshot,
                    { screen = AppScreen.CONTROL_CENTER },
                    { RouterController.openMobileNetworkSettings(context) },
                    { RouterController.openSimSettings(context) },
                    { RouterController.openApnSettings(context) },
                    { RouterController.openVpnSettings(context) },
                    { RouterController.openPrivateDnsSettings(context) },
                    { RouterController.openBatteryOptimizationSettings(context) },
                )

                AppScreen.BACKUP_RESTORE -> BackupRestoreScreen(
                    preferences.exportJson(),
                    { raw ->
                        val ok = preferences.importJson(raw)
                        if (ok) {
                            settings = readSettings(preferences)
                            activity?.setKeepScreenOn(settings.keepScreenOn)
                            activity?.setImmersiveMode(settings.immersiveMode)
                        }
                        ok
                    },
                    { RouterController.shareText(context, "ZeroFi backup", it) },
                    { screen = AppScreen.SETTINGS },
                )

                AppScreen.DIAGNOSTICS -> DiagnosticsScreenV2(
                    snapshot, featureSnapshot, hotspotActive, clientSnapshot,
                    detectedBand ?: settings.preferredBand,
                ) { screen = AppScreen.CONTROL_CENTER }

                AppScreen.STANDBY -> StandbyScreenV2(
                    snapshot, hotspotActive, clientSnapshot, snapshot.monthBytes,
                    settings.dataLimitGb, settings.oledSaver || settings.autoOledOnStandby,
                ) { screen = AppScreen.DASHBOARD }
            }
        }
    }
}
