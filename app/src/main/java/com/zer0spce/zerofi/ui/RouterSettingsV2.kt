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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zer0spce.zerofi.BuildConfig

@Composable
fun RouterSettingsV2(
    settings: ZeroFiSettingsState,
    deviceOwner: Boolean,
    lockRunning: Boolean,
    dedicatedModeSupported: Boolean,
    automationConfigured: Boolean,
    onBack: () -> Unit,
    onAutoLaunchChanged: (Boolean) -> Unit,
    onRouterLockToggle: () -> Unit,
    onDedicatedBatterySaverChanged: (Boolean) -> Unit,
    onOpenDisplay: () -> Unit,
    onOledSaverChanged: (Boolean) -> Unit,
    onAutomationClick: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onDiagnostics: () -> Unit,
    onStandby: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SettingsNavButton(Icons.Rounded.ArrowBack, "Back", onBack)
            Spacer(Modifier.width(10.dp))
            Column {
                Text("Router settings", color = MaterialTheme.colorScheme.onBackground, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                Text("ZeroFi appliance controls", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
            }
        }
        Spacer(Modifier.height(10.dp))
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SettingsCard {
                SettingsLabel("ROUTER MODE")
                Spacer(Modifier.height(6.dp))
                SettingsSwitch(
                    "Launch after boot",
                    "Open ZeroFi after Android starts",
                    settings.autoLaunchOnBoot,
                    onChecked = onAutoLaunchChanged,
                )
                SettingsSwitch(
                    "Dedicated Battery Saver",
                    if (dedicatedModeSupported) "Suspend ordinary user apps while ZeroFi runs" else "Requires Android 7+ and Device Owner",
                    settings.dedicatedBatterySaver,
                    enabled = dedicatedModeSupported,
                    onChecked = onDedicatedBatterySaverChanged,
                )
                Spacer(Modifier.height(6.dp))
                SettingsAction(if (lockRunning) "Exit Router Lock" else "Enable Router Lock", Icons.Rounded.Lock, onRouterLockToggle)
                Spacer(Modifier.height(7.dp))
                SettingsAction("Standby display", Icons.Rounded.Bedtime, onStandby)
                Spacer(Modifier.height(7.dp))
                SettingsAction("Router diagnostics", Icons.Rounded.Info, onDiagnostics)
            }

            SettingsCard {
                SettingsLabel("DISPLAY")
                Spacer(Modifier.height(6.dp))
                SettingsAction("Display settings", Icons.Rounded.Brightness6, onOpenDisplay)
                Spacer(Modifier.height(4.dp))
                SettingsSwitch("OLED Saver", "Use true-black surfaces on OLED screens", settings.oledSaver, onChecked = onOledSaverChanged)
                SettingsStatus("Theme", if (settings.darkMode) "Dark" else "Light")
            }

            SettingsCard {
                SettingsLabel("HOTSPOT AUTOMATION")
                Spacer(Modifier.height(6.dp))
                val automationOn = settings.hotspotAutomationEnabled && automationConfigured
                SettingsAction(
                    if (automationOn) "Hotspot automation: ON" else "Set up hotspot automation",
                    Icons.Rounded.Settings,
                    onAutomationClick,
                )
                Spacer(Modifier.height(7.dp))
                Text(
                    when {
                        automationOn -> "Configured. Tap above to disable automation."
                        settings.hotspotAutomationEnabled -> "Accessibility setup is still required."
                        else -> "Optional. Used for Quick Settings hotspot control and applying SSID/password on Android Settings."
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.sp,
                    lineHeight = 13.sp,
                )
                Spacer(Modifier.height(8.dp))
                SettingsStatus("Wi-Fi QR", if (settings.hotspotProfileConfirmed) "Verified" else "Not verified")
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
                    modifier = Modifier.fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .55f), RoundedCornerShape(12.dp))
                        .padding(10.dp),
                )
                Spacer(Modifier.height(9.dp))
                SettingsAction("Open ZeroFi App Settings", Icons.Rounded.Settings, onOpenAppSettings)
                Spacer(Modifier.height(8.dp))
                Text("ZeroFi ${BuildConfig.VERSION_NAME}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        Modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, shape)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .15f), shape)
            .padding(14.dp),
        content = content,
    )
}

@Composable private fun SettingsLabel(text: String) {
    Text(text, color = MaterialTheme.colorScheme.primary, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
}

@Composable private fun SettingsSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onChecked: (Boolean) -> Unit,
) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.5.sp, lineHeight = 12.sp)
        }
        Switch(checked = checked, onCheckedChange = onChecked, enabled = enabled)
    }
}

@Composable private fun SettingsAction(text: String, icon: ImageVector, onClick: () -> Unit) {
    val shape = RoundedCornerShape(13.dp)
    Row(
        Modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary.copy(alpha = .09f), shape)
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .28f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(7.dp))
        Text(text, color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable private fun SettingsStatus(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, modifier = Modifier.weight(1f))
        Text(value, color = MaterialTheme.colorScheme.onSurface, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable private fun SettingsNavButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        Modifier.background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, label, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}
