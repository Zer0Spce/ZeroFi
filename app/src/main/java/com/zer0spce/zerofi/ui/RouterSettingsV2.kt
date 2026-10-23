package com.zer0spce.zerofi.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zer0spce.zerofi.BuildConfig
import kotlin.math.roundToInt

@Composable
fun RouterSettingsV2(
    settings: ZeroFiSettingsState,
    deviceOwner: Boolean,
    lockRunning: Boolean,
    dedicatedModeSupported: Boolean,
    automationConfigured: Boolean,
    notificationsGranted: Boolean,
    onBack: () -> Unit,
    onAutoLaunchChanged: (Boolean) -> Unit,
    onRestoreLockChanged: (Boolean) -> Unit,
    onRouterLockToggle: () -> Unit,
    onDedicatedBatterySaverChanged: (Boolean) -> Unit,
    onOpenDisplay: () -> Unit,
    onOledSaverChanged: (Boolean) -> Unit,
    onAutoOledChanged: (Boolean) -> Unit,
    onAutomationClick: () -> Unit,
    onStartupAutomationChanged: (Boolean) -> Unit,
    onRunStartupAutomation: () -> Unit,
    onRouterNotificationChanged: (Boolean) -> Unit,
    onAutoOffChanged: (Int) -> Unit,
    onKeepWhileChargingChanged: (Boolean) -> Unit,
    onDataAlertsChanged: (Boolean) -> Unit,
    onThermalAlertsChanged: (Boolean) -> Unit,
    onThermalThresholdChanged: (Float) -> Unit,
    onRequestNotifications: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onControlCenter: () -> Unit,
    onAutomationRules: () -> Unit,
    onCellularDiagnostics: () -> Unit,
    onInternetHealth: () -> Unit,
    onSignalHistory: () -> Unit,
    onSpeedTest: () -> Unit,
    onNetworkTools: () -> Unit,
    onBackupRestore: () -> Unit,
    onDiagnostics: () -> Unit,
    onStandby: () -> Unit,
) {
    var autoOff by remember(settings.autoOffMinutes) { mutableFloatStateOf(settings.autoOffMinutes.toFloat()) }
    var temp by remember(settings.thermalThresholdC) { mutableFloatStateOf(settings.thermalThresholdC) }
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SettingsNavButton(Icons.Rounded.ArrowBack, "Back", onBack)
            Spacer(Modifier.width(10.dp))
            Column {
                Text("Router settings", color = MaterialTheme.colorScheme.onBackground, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                Text("ZeroFi 1.2 appliance controls", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
            }
        }
        Spacer(Modifier.height(10.dp))
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SettingsCard {
                SettingsLabel("ROUTER MODE")
                Spacer(Modifier.height(6.dp))
                SettingsSwitch("Launch after boot", "Open ZeroFi after Android starts", settings.autoLaunchOnBoot, onChecked = onAutoLaunchChanged)
                SettingsSwitch("Restore Router Lock", "Re-enter router lock when ZeroFi launches", settings.restoreRouterLock, onChecked = onRestoreLockChanged)
                SettingsSwitch(
                    "Dedicated Battery Saver",
                    if (dedicatedModeSupported) "Suspend ordinary user apps while ZeroFi runs" else "Requires Android 7+ and Device Owner",
                    settings.dedicatedBatterySaver,
                    enabled = dedicatedModeSupported,
                    onChecked = onDedicatedBatterySaverChanged,
                )
                Spacer(Modifier.height(6.dp))
                SettingsAction(if (lockRunning) "Exit Router Lock" else "Enable Router Lock", Icons.Rounded.Lock, onRouterLockToggle)
                Spacer(Modifier.height(7.dp)); SettingsAction("Standby display", Icons.Rounded.Bedtime, onStandby)
                Spacer(Modifier.height(7.dp)); SettingsAction("Router diagnostics", Icons.Rounded.HealthAndSafety, onDiagnostics)
            }

            SettingsCard {
                SettingsLabel("DISPLAY")
                Spacer(Modifier.height(6.dp))
                SettingsAction("Display settings", Icons.Rounded.Brightness6, onOpenDisplay)
                Spacer(Modifier.height(4.dp))
                SettingsSwitch("OLED Saver", "Use true-black surfaces on OLED screens", settings.oledSaver, onChecked = onOledSaverChanged)
                SettingsSwitch("OLED standby", "Use true black automatically on the standby screen", settings.autoOledOnStandby, onChecked = onAutoOledChanged)
                SettingsStatus("Theme", if (settings.darkMode) "Dark" else "Light")
            }

            SettingsCard {
                SettingsLabel("HOTSPOT AUTOMATION")
                Spacer(Modifier.height(6.dp))
                val automationOn = settings.hotspotAutomationEnabled && automationConfigured
                SettingsAction(if (automationOn) "Hotspot automation: ON" else "Set up hotspot automation", Icons.Rounded.Settings, onAutomationClick)
                Spacer(Modifier.height(6.dp))
                SettingsSwitch(
                    "Prepare router when ZeroFi opens",
                    "Wi-Fi off → mobile data on → hotspot on",
                    settings.startupRouterAutomationEnabled,
                    enabled = automationOn,
                    onChecked = onStartupAutomationChanged,
                )
                if (automationOn) {
                    Spacer(Modifier.height(6.dp)); SettingsAction("Run startup sequence now", Icons.Rounded.PlayArrow, onRunStartupAutomation)
                }
                Spacer(Modifier.height(9.dp))
                Text(if (settings.autoOffMinutes == 0) "Auto-off: Disabled" else "Auto-off after no clients: ${settings.autoOffMinutes} min", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 10.sp)
                Slider(autoOff, {
                    autoOff = it
                    val value = when (it.roundToInt()) { 0 -> 0; in 1..7 -> 5; in 8..19 -> 10; in 20..44 -> 30; else -> 60 }
                    onAutoOffChanged(value)
                }, valueRange = 0f..60f)
                Text("Auto-off only runs when ZeroFi can verify the client count. It never guesses.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp)
                SettingsSwitch("Keep hotspot while charging", "Never auto-off the hotspot while the phone is charging", settings.keepHotspotWhileCharging, onChecked = onKeepWhileChargingChanged)
                Spacer(Modifier.height(7.dp)); SettingsAction("Quiet hours & charging rules", Icons.Rounded.Bedtime, onAutomationRules)
                Spacer(Modifier.height(4.dp)); SettingsStatus("Wi-Fi QR", if (settings.hotspotProfileConfirmed) "Verified" else "Not verified")
            }

            SettingsCard {
                SettingsLabel("ROUTER STATUS")
                Spacer(Modifier.height(6.dp))
                SettingsSwitch("Persistent router notification", "Hotspot, network, clients, battery and data status", settings.routerNotificationEnabled, onChecked = onRouterNotificationChanged)
                SettingsStatus("Notification permission", if (notificationsGranted) "Granted" else "Not granted")
                if (!notificationsGranted) { Spacer(Modifier.height(6.dp)); SettingsAction("Allow router notifications", Icons.Rounded.Notifications, onRequestNotifications) }
                Spacer(Modifier.height(6.dp))
                Text("A ZeroFi Quick Settings tile is included. Add it from Android's Edit tiles screen for one-tap hotspot control.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp, lineHeight = 11.sp)
            }

            SettingsCard {
                SettingsLabel("ALERTS & THERMALS")
                Spacer(Modifier.height(6.dp))
                SettingsSwitch("Data alerts", "Notify at 50%, 75%, 90% and 100% of allowance", settings.dataAlertsEnabled, onChecked = onDataAlertsChanged)
                SettingsSwitch("Temperature alerts", "Warn when battery temperature gets too high", settings.thermalAlertsEnabled, onChecked = onThermalAlertsChanged)
                Text("Temperature warning: ${temp.roundToInt()}°C", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 10.sp)
                Slider(temp, { temp = it; onThermalThresholdChanged(it) }, valueRange = 35f..50f, steps = 14)
            }

            SettingsCard {
                SettingsLabel("ROUTER TOOLS")
                Spacer(Modifier.height(6.dp))
                SettingsAction("Router Control Center", Icons.Rounded.Dashboard, onControlCenter)
                Spacer(Modifier.height(7.dp)); SettingsAction("Cellular diagnostics", Icons.Rounded.NetworkCell, onCellularDiagnostics)
                Spacer(Modifier.height(7.dp)); SettingsAction("Internet health & recovery", Icons.Rounded.Public, onInternetHealth)
                Spacer(Modifier.height(7.dp)); SettingsAction("Signal history", Icons.Rounded.SignalCellularAlt, onSignalHistory)
                Spacer(Modifier.height(7.dp)); SettingsAction("Speed test & history", Icons.Rounded.Speed, onSpeedTest)
                Spacer(Modifier.height(7.dp)); SettingsAction("Network tools", Icons.Rounded.SettingsEthernet, onNetworkTools)
                Spacer(Modifier.height(7.dp)); SettingsAction("Backup & restore", Icons.Rounded.Backup, onBackupRestore)
            }

            SettingsCard {
                SettingsLabel("DEVICE OWNER")
                Spacer(Modifier.height(6.dp))
                SettingsStatus("Device Owner", if (deviceOwner) "Configured" else "Not configured")
                SettingsStatus("Router Lock", if (lockRunning) "Active" else "Inactive")
                Spacer(Modifier.height(8.dp))
                Text(
                    "adb shell dpm set-device-owner\ncom.zer0spce.zerofi/.admin.ZeroFiDeviceAdminReceiver",
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .55f), RoundedCornerShape(12.dp)).padding(10.dp),
                )
                Spacer(Modifier.height(9.dp)); SettingsAction("Open ZeroFi App Settings", Icons.Rounded.Settings, onOpenAppSettings)
                Spacer(Modifier.height(8.dp)); Text("ZeroFi ${BuildConfig.VERSION_NAME}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, shape).border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .15f), shape).padding(14.dp), content = content)
}
@Composable private fun SettingsLabel(text: String) { Text(text, color = MaterialTheme.colorScheme.primary, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp) }
@Composable private fun SettingsSwitch(title: String, subtitle: String, checked: Boolean, enabled: Boolean = true, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(title, color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.SemiBold); Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.5.sp, lineHeight = 12.sp) }
        Switch(checked = checked, onCheckedChange = onChecked, enabled = enabled)
    }
}
@Composable private fun SettingsAction(text: String, icon: ImageVector, onClick: () -> Unit) {
    val shape = RoundedCornerShape(13.dp)
    Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primary.copy(alpha = .09f), shape).border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .28f), shape).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(17.dp)); Spacer(Modifier.width(7.dp)); Text(text, color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}
@Composable private fun SettingsStatus(label: String, value: String) { Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) { Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, modifier = Modifier.weight(1f)); Text(value, color = MaterialTheme.colorScheme.onSurface, fontSize = 9.sp, fontWeight = FontWeight.SemiBold) } }
@Composable private fun SettingsNavButton(icon: ImageVector, label: String, onClick: () -> Unit) { Row(Modifier.background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, label, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(17.dp)); Spacer(Modifier.width(6.dp)); Text(label, color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp, fontWeight = FontWeight.SemiBold) } }
