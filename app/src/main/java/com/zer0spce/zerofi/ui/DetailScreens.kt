package com.zer0spce.zerofi.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Brightness6
import androidx.compose.material.icons.rounded.DataUsage
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zer0spce.zerofi.BuildConfig
import com.zer0spce.zerofi.platform.SystemSnapshot
import kotlin.math.roundToInt

@Composable
fun HotspotScreen(
    active: Boolean?,
    detectedBand: String?,
    ssid: String,
    password: String,
    security: String,
    preferredBand: String,
    onBack: () -> Unit,
    onOpenSystemHotspot: () -> Unit,
    onSaveProfile: (String, String, String, String) -> Unit,
) {
    var localSsid by remember(ssid) { mutableStateOf(ssid) }
    var localPassword by remember(password) { mutableStateOf(password) }
    var localSecurity by remember(security) { mutableStateOf(security) }
    var localBand by remember(preferredBand) { mutableStateOf(preferredBand) }

    DetailPage("Hotspot", "Internet sharing & connection card", onBack) {
        ResponsiveCards(
            firstWeight = 1f,
            secondWeight = 1.4f,
            first = {
                SectionLabel("ANDROID HOTSPOT")
                Spacer(Modifier.height(10.dp))
                BigStatus(
                    title = when (active) {
                        true -> "Hotspot is on"
                        false -> "Hotspot is off"
                        null -> "System managed"
                    },
                    subtitle = detectedBand ?: "Status visibility depends on Android/OEM",
                    positive = active == true,
                )
                Spacer(Modifier.height(12.dp))
                InfoText(
                    "ZeroFi uses the phone's real tethering. On devices where Android restricts direct control, ZeroFi uses its configured hotspot automation or opens the manufacturer's controls.",
                )
                Spacer(Modifier.height(14.dp))
                ActionButton("Open Android Hotspot Controls", Icons.Rounded.OpenInNew, onOpenSystemHotspot)
            },
            second = {
                SectionLabel("CONNECTION CARD")
                Spacer(Modifier.height(8.dp))
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val stacked = maxWidth < 520.dp
                    if (stacked) {
                        Column {
                            HotspotFields(
                                localSsid = localSsid,
                                localPassword = localPassword,
                                localSecurity = localSecurity,
                                localBand = localBand,
                                onSsid = { localSsid = it.take(32) },
                                onPassword = { localPassword = it.take(63) },
                                onSecurity = { localSecurity = it },
                                onBand = { localBand = it },
                            )
                            Spacer(Modifier.height(14.dp))
                            QrConnectionCard(localSsid, localPassword, localSecurity)
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Column(Modifier.weight(1f)) {
                                HotspotFields(
                                    localSsid = localSsid,
                                    localPassword = localPassword,
                                    localSecurity = localSecurity,
                                    localBand = localBand,
                                    onSsid = { localSsid = it.take(32) },
                                    onPassword = { localPassword = it.take(63) },
                                    onSecurity = { localSecurity = it },
                                    onBand = { localBand = it },
                                )
                            }
                            QrConnectionCard(localSsid, localPassword, localSecurity)
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                ActionButton("Save Connection Card", Icons.Rounded.Save) {
                    onSaveProfile(localSsid, localPassword, localSecurity, localBand)
                }
            },
        )
    }
}

@Composable
private fun HotspotFields(
    localSsid: String,
    localPassword: String,
    localSecurity: String,
    localBand: String,
    onSsid: (String) -> Unit,
    onPassword: (String) -> Unit,
    onSecurity: (String) -> Unit,
    onBand: (String) -> Unit,
) {
    OutlinedTextField(
        value = localSsid,
        onValueChange = onSsid,
        label = { Text("Wi-Fi name (SSID)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(7.dp))
    OutlinedTextField(
        value = localPassword,
        onValueChange = onPassword,
        label = { Text("Password") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(9.dp))
    SmallLabel("Security")
    ChoiceRow(listOf("WPA", "nopass"), localSecurity, onSecurity)
    Spacer(Modifier.height(8.dp))
    SmallLabel("Preferred band")
    ChoiceRow(listOf("System", "2.4 GHz", "5 GHz"), localBand, onBand)
}

@Composable
private fun QrConnectionCard(ssid: String, password: String, security: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val bitmap = remember(ssid, password, security) {
            generateQrBitmap(wifiQrPayload(ssid, password, security), 320)
        }
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Wi-Fi QR code",
                modifier = Modifier
                    .size(142.dp)
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .padding(7.dp),
            )
        }
        Spacer(Modifier.height(5.dp))
        Text("Scan to connect", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun DevicesScreen(
    hotspotActive: Boolean?,
    onBack: () -> Unit,
    onOpenHotspotSettings: () -> Unit,
) {
    DetailPage("Connected devices", "Hotspot client visibility", onBack) {
        ResponsiveCards(
            first = {
                SectionLabel("CLIENT STATUS")
                Spacer(Modifier.height(12.dp))
                BigStatus(
                    title = when (hotspotActive) {
                        true -> "Hotspot active"
                        false -> "Hotspot inactive"
                        null -> "Client list restricted"
                    },
                    subtitle = "ZeroFi never invents a connected-device count.",
                    positive = hotspotActive == true,
                )
                Spacer(Modifier.height(12.dp))
                InfoText(
                    "Many Android manufacturers keep the hotspot client table behind system permissions. When the phone exposes it, ZeroFi can show it; otherwise Android's hotspot screen remains the source of truth.",
                )
                Spacer(Modifier.height(14.dp))
                ActionButton("Open Hotspot Device List", Icons.Rounded.Devices, onOpenHotspotSettings)
            },
            second = {
                SectionLabel("COMPATIBILITY")
                Spacer(Modifier.height(12.dp))
                FeatureLine("Android 6+", "Dashboard and router shell")
                FeatureLine("Android 7+", "Device Owner app suspension")
                FeatureLine("Android 16+", "Richer tethering visibility")
                FeatureLine("OEM dependent", "Client names, IP/MAC and hotspot details")
                Spacer(Modifier.height(12.dp))
                InfoText("Unsupported information is shown as unavailable rather than fabricated.")
            },
        )
    }
}

@Composable
fun DataScreen(
    snapshot: SystemSnapshot,
    limitGb: Float,
    cycleDay: Int,
    onBack: () -> Unit,
    onOpenUsageAccess: () -> Unit,
    onOpenSystemDataUsage: () -> Unit,
    onLimitChanged: (Float) -> Unit,
    onCycleDayChanged: (Int) -> Unit,
) {
    var localLimit by remember(limitGb) { mutableFloatStateOf(limitGb.coerceIn(1f, 500f)) }
    var localDay by remember(cycleDay) { mutableFloatStateOf(cycleDay.toFloat()) }

    DetailPage("Mobile data", "Usage, allowance & billing cycle", onBack) {
        ResponsiveCards(
            first = {
                SectionLabel("CURRENT USAGE")
                Spacer(Modifier.height(12.dp))
                FeatureLine("This cycle", snapshot.monthBytes?.let(::formatBytes) ?: "Unavailable")
                FeatureLine("Last 3 days", snapshot.threeDayBytes?.let(::formatBytes) ?: "Unavailable")
                FeatureLine("Allowance", "${limitGb.roundToInt()} GB")
                FeatureLine("Cycle reset", "Day $cycleDay")
                Spacer(Modifier.height(12.dp))
                if (!snapshot.usageAccessGranted) {
                    InfoText("Usage Access is required for device-wide mobile-data counters.")
                    Spacer(Modifier.height(8.dp))
                    ActionButton("Grant Usage Access", Icons.Rounded.DataUsage, onOpenUsageAccess)
                    Spacer(Modifier.height(8.dp))
                }
                ActionButton("Open Android Data Usage", Icons.Rounded.OpenInNew, onOpenSystemDataUsage)
            },
            second = {
                SectionLabel("ZEROFI DATA LIMIT")
                Spacer(Modifier.height(12.dp))
                Text(
                    "Monthly allowance: ${localLimit.roundToInt()} GB",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
                Slider(
                    value = localLimit,
                    onValueChange = {
                        localLimit = it
                        onLimitChanged(it)
                    },
                    valueRange = 1f..500f,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Reset day: ${localDay.roundToInt()}",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
                Slider(
                    value = localDay,
                    onValueChange = {
                        localDay = it
                        onCycleDayChanged(it.roundToInt().coerceIn(1, 28))
                    },
                    valueRange = 1f..28f,
                    steps = 26,
                )
                Spacer(Modifier.height(8.dp))
                InfoText("ZeroFi tracks usage for display and warnings; it does not change your carrier plan or billing.")
            },
        )
    }
}

@Composable
fun DisplayScreen(
    keepScreenOn: Boolean,
    immersiveMode: Boolean,
    oledSaver: Boolean,
    onBack: () -> Unit,
    onKeepScreenOnChanged: (Boolean) -> Unit,
    onImmersiveModeChanged: (Boolean) -> Unit,
    onOledSaverChanged: (Boolean) -> Unit,
    onBrightnessChanged: (Float) -> Unit,
    onSystemBrightness: () -> Unit,
) {
    var brightness by remember { mutableFloatStateOf(if (oledSaver) 0.30f else 0.65f) }

    DetailPage("Display", "Always-on router screen controls", onBack) {
        ResponsiveCards(
            first = {
                SectionLabel("SCREEN BEHAVIOR")
                Spacer(Modifier.height(8.dp))
                SettingSwitch(
                    title = "Keep screen awake",
                    subtitle = "Keep ZeroFi visible while the phone acts as a router",
                    checked = keepScreenOn,
                    onCheckedChange = onKeepScreenOnChanged,
                )
                SettingSwitch(
                    title = "Immersive fullscreen",
                    subtitle = "Hide status and navigation bars",
                    checked = immersiveMode,
                    onCheckedChange = onImmersiveModeChanged,
                )
                SettingSwitch(
                    title = "OLED Saver",
                    subtitle = "True black UI, lower glow and anti-burn-in pixel drift",
                    checked = oledSaver,
                    onCheckedChange = onOledSaverChanged,
                )
            },
            second = {
                SectionLabel("BRIGHTNESS")
                Spacer(Modifier.height(12.dp))
                Text(
                    "ZeroFi brightness: ${(brightness * 100).roundToInt()}%",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
                Slider(
                    value = brightness,
                    onValueChange = {
                        brightness = it
                        onBrightnessChanged(it)
                    },
                    valueRange = 0.05f..1f,
                )
                InfoText(
                    if (oledSaver) {
                        "OLED Saver keeps unused pixels black, reduces glow, and shifts the dashboard slightly over time to reduce static burn-in risk."
                    } else {
                        "Lowering brightness is usually the biggest display-side battery saving for an always-on router dashboard."
                    },
                )
                Spacer(Modifier.height(12.dp))
                ActionButton("Use System Brightness", Icons.Rounded.Brightness6, onSystemBrightness)
            },
        )
    }
}

@Composable
fun SettingsScreen(
    settings: ZeroFiSettingsState,
    deviceOwner: Boolean,
    lockRunning: Boolean,
    dedicatedModeSupported: Boolean,
    onBack: () -> Unit,
    onAutoLaunchChanged: (Boolean) -> Unit,
    onRouterLockToggle: () -> Unit,
    onDedicatedBatterySaverChanged: (Boolean) -> Unit,
    onOpenAppSettings: () -> Unit,
) {
    DetailPage("Router settings", "Dedicated-device controls", onBack) {
        ResponsiveCards(
            first = {
                SectionLabel("DEDICATED ROUTER MODE")
                Spacer(Modifier.height(8.dp))
                SettingSwitch(
                    title = "Launch after boot",
                    subtitle = "Open ZeroFi after Android finishes starting",
                    checked = settings.autoLaunchOnBoot,
                    onCheckedChange = onAutoLaunchChanged,
                )
                SettingSwitch(
                    title = "Dedicated Battery Saver",
                    subtitle = if (dedicatedModeSupported) {
                        "Suspend ordinary user apps while ZeroFi runs; system apps stay untouched"
                    } else {
                        "Requires Android 7+ and ZeroFi configured as Device Owner"
                    },
                    checked = settings.dedicatedBatterySaver,
                    enabled = dedicatedModeSupported,
                    onCheckedChange = onDedicatedBatterySaverChanged,
                )
                Spacer(Modifier.height(8.dp))
                ActionButton(
                    text = if (lockRunning) "Exit Router Lock" else "Enable Router Lock",
                    icon = Icons.Rounded.Lock,
                    onClick = onRouterLockToggle,
                )
                Spacer(Modifier.height(12.dp))
                FeatureLine("Device Owner", if (deviceOwner) "Configured" else "Not configured")
                FeatureLine("Router Lock", if (lockRunning) "Active" else "Inactive")
                FeatureLine("OLED Saver", if (settings.oledSaver) "On" else "Off")
            },
            second = {
                SectionLabel("DEVICE OWNER SETUP")
                Spacer(Modifier.height(9.dp))
                InfoText(
                    "Full kiosk mode and user-app suspension require ZeroFi to be Device Owner. This is intended for a spare/dedicated phone and normally must be configured before accounts are added or after a factory reset.",
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "adb shell dpm set-device-owner\ncom.zer0spce.zerofi/.admin.ZeroFiDeviceAdminReceiver",
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                        .padding(10.dp),
                )
                Spacer(Modifier.height(10.dp))
                InfoText(
                    "Dedicated Battery Saver deliberately ignores Android system apps and only asks Device Policy to suspend ordinary installed user apps.",
                )
                Spacer(Modifier.height(12.dp))
                ActionButton("Open ZeroFi App Settings", Icons.Rounded.Settings, onOpenAppSettings)
                Spacer(Modifier.height(8.dp))
                Text(
                    "ZeroFi ${BuildConfig.VERSION_NAME}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.sp,
                )
            },
        )
    }
}

@Composable
private fun DetailPage(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButtonTile(Icons.Rounded.ArrowBack, "Back", onBack)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    subtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Box(Modifier.weight(1f).fillMaxWidth()) { content() }
    }
}

@Composable
private fun ResponsiveCards(
    firstWeight: Float = 1f,
    secondWeight: Float = 1f,
    first: @Composable ColumnScope.() -> Unit,
    second: @Composable ColumnScope.() -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val portrait = maxHeight > maxWidth
        if (portrait) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SurfaceCard(Modifier.fillMaxWidth(), first)
                SurfaceCard(Modifier.fillMaxWidth(), second)
                Spacer(Modifier.height(4.dp))
            }
        } else {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SurfaceCard(Modifier.weight(firstWeight).fillMaxHeight(), first)
                SurfaceCard(Modifier.weight(secondWeight).fillMaxHeight(), second)
            }
        }
    }
}

@Composable
private fun SurfaceCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = modifier
            .shadow(3.dp, shape)
            .background(MaterialTheme.colorScheme.surface, shape)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.20f), shape)
            .padding(14.dp),
        content = content,
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        color = MaterialTheme.colorScheme.primary,
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.1.sp,
    )
}

@Composable
private fun SmallLabel(text: String) {
    Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
}

@Composable
private fun BigStatus(title: String, subtitle: String, positive: Boolean) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(10.dp)
                .background(
                    if (positive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    CircleShape,
                ),
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
        }
    }
}

@Composable
private fun InfoText(text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        Icon(
            Icons.Rounded.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(15.dp),
        )
        Text(
            text,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp,
            lineHeight = 14.sp,
        )
    }
}

@Composable
private fun FeatureLine(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, modifier = Modifier.weight(1f))
        Text(
            value,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SettingSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 9.sp,
                lineHeight = 12.sp,
            )
        }
        Spacer(Modifier.width(10.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@Composable
private fun ChoiceRow(values: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        values.forEach { value ->
            val isSelected = value == selected
            Text(
                text = value,
                color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(9.dp),
                    )
                    .border(
                        1.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                        RoundedCornerShape(9.dp),
                    )
                    .clickable { onSelect(value) }
                    .padding(horizontal = 9.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun ActionButton(text: String, icon: ImageVector, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale = if (pressed) 0.98f else 1f
    val shape = RoundedCornerShape(13.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .background(MaterialTheme.colorScheme.primary.copy(alpha = if (pressed) 0.15f else 0.09f), shape)
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f), shape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(7.dp))
        Text(text, color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun IconButtonTile(icon: ImageVector, label: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface, shape)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.20f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}
