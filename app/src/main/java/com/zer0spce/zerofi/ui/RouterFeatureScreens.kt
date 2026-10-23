package com.zer0spce.zerofi.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zer0spce.zerofi.platform.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

data class SignalSample(val timeMs: Long, val dbm: Int)

@Composable
fun RouterControlCenterScreen(
    snapshot: SystemSnapshot,
    feature: RouterFeatureSnapshot,
    hotspotActive: Boolean?,
    hotspotBand: String,
    clientSnapshot: HotspotClientSnapshot,
    settings: ZeroFiSettingsState,
    onBack: () -> Unit,
    onNavigate: (AppScreen) -> Unit,
    onToggleHotspot: () -> Unit,
) {
    FeaturePage("Router Control Center", "Live ZeroFi router overview", onBack) {
        FeatureCard {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                FeatureCircle(Icons.Rounded.Wifi, if (hotspotActive == true) Color(0xFF35D97B) else MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(settings.hotspotSsid, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                    Text("${snapshot.networkType} · $hotspotBand · ${snapshot.carrierName}", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                FilledTonalButton(onClick = onToggleHotspot) { Text(if (hotspotActive == true) "Turn off" else "Turn on") }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricBox(Modifier.weight(1f), "Clients", if (clientSnapshot.available) clientSnapshot.clients.size.toString() else "—", if (clientSnapshot.available) clientSnapshot.source else "Restricted")
                MetricBox(Modifier.weight(1f), "Battery", snapshot.batteryPercent?.let { "$it%" } ?: "—", if (snapshot.charging) "Charging" else snapshot.batteryTimeRemainingText)
                MetricBox(Modifier.weight(1f), "Signal", snapshot.signalDbm?.let { "$it dBm" } ?: "—", snapshot.networkType)
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricBox(Modifier.weight(1f), "Download", snapshot.downloadBytesPerSecond?.let(::featureRate) ?: "—", "Live")
                MetricBox(Modifier.weight(1f), "Upload", snapshot.uploadBytesPerSecond?.let(::featureRate) ?: "—", "Live")
                MetricBox(Modifier.weight(1f), "Data", snapshot.monthBytes?.let(::featureBytes) ?: "—", "This cycle")
            }
        }
        FeatureGrid(
            listOf(
                FeatureAction(Icons.Rounded.Wifi, "Wi-Fi", "QR & hotspot") { onNavigate(AppScreen.HOTSPOT) },
                FeatureAction(Icons.Rounded.Devices, "Clients", "Connected devices") { onNavigate(AppScreen.DEVICES) },
                FeatureAction(Icons.Rounded.DataUsage, "Data", "Usage & alerts") { onNavigate(AppScreen.DATA) },
                FeatureAction(Icons.Rounded.SignalCellularAlt, "Signal", "History") { onNavigate(AppScreen.SIGNAL_HISTORY) },
                FeatureAction(Icons.Rounded.Speed, "Speed test", "Manual quick test") { onNavigate(AppScreen.SPEED_TEST) },
                FeatureAction(Icons.Rounded.SettingsEthernet, "Network", "SIM, APN, VPN, DNS") { onNavigate(AppScreen.NETWORK_TOOLS) },
                FeatureAction(Icons.Rounded.HealthAndSafety, "Diagnostics", "Router health") { onNavigate(AppScreen.DIAGNOSTICS) },
                FeatureAction(Icons.Rounded.Backup, "Backup", "Export & restore") { onNavigate(AppScreen.BACKUP_RESTORE) },
            )
        )
        FeatureCard {
            FeatureLine("Internet", if (snapshot.validatedInternet) "Validated" else "Not validated")
            FeatureLine("Local IP", feature.localIp)
            FeatureLine("Gateway", feature.gatewayIp)
            FeatureLine("VPN", if (feature.vpnActive) "Active" else "Off")
            FeatureLine("Metered", if (feature.metered) "Yes" else "No")
        }
    }
}

@Composable
fun ClientManagementScreen(
    snapshot: HotspotClientSnapshot,
    trustedMacs: Set<String>,
    aliasFor: (String) -> String?,
    onToggleTrusted: (String) -> Unit,
    onRename: (String, String) -> Unit,
    onOpenSystemClients: () -> Unit,
    onBack: () -> Unit,
) {
    var renameClient by remember { mutableStateOf<HotspotClient?>(null) }
    var renameText by remember { mutableStateOf("") }
    FeaturePage("Connected devices", "Client visibility depends on Android/OEM", onBack) {
        FeatureCard {
            Text(if (snapshot.available) "${snapshot.clients.size} connected" else "Client list restricted", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            Text(if (snapshot.available) "Source: ${snapshot.source}" else "ZeroFi will not fabricate a client count. Open Android's hotspot device list for the system view.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
            Spacer(Modifier.height(10.dp))
            FeatureButton("Open Android client controls", Icons.Rounded.OpenInNew, onOpenSystemClients)
        }
        if (snapshot.available && snapshot.clients.isEmpty()) {
            FeatureCard { Text("No tethered clients found.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        snapshot.clients.forEach { client ->
            val trusted = client.macAddress.lowercase() in trustedMacs
            FeatureCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FeatureCircle(Icons.Rounded.Devices, if (trusted) Color(0xFF35D97B) else MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(aliasFor(client.macAddress) ?: "Hotspot client", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(client.ipAddress, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                        Text(client.macAddress, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp)
                    }
                    TextButton(onClick = {
                        renameClient = client
                        renameText = aliasFor(client.macAddress).orEmpty()
                    }) { Text("Rename") }
                    TextButton(onClick = { onToggleTrusted(client.macAddress) }) { Text(if (trusted) "Untrust" else "Trust") }
                }
            }
        }
        FeatureCard {
            Text("Block / kick", fontWeight = FontWeight.Bold)
            Text("Android does not expose a reliable public block/kick API to ordinary apps. ZeroFi only routes to the OEM client controls instead of showing a button that may not work.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
        }
    }
    val client = renameClient
    if (client != null) {
        AlertDialog(
            onDismissRequest = { renameClient = null },
            title = { Text("Rename client") },
            text = { OutlinedTextField(renameText, { renameText = it.take(40) }, label = { Text("Device name") }, singleLine = true) },
            confirmButton = { TextButton(onClick = { onRename(client.macAddress, renameText); renameClient = null }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { renameClient = null }) { Text("Cancel") } },
        )
    }
}

@Composable
fun DataUsageV2Screen(
    snapshot: SystemSnapshot,
    feature: RouterFeatureSnapshot,
    settings: ZeroFiSettingsState,
    notificationsGranted: Boolean,
    onBack: () -> Unit,
    onLimitChanged: (Float) -> Unit,
    onCycleDayChanged: (Int) -> Unit,
    onAlertsChanged: (Boolean) -> Unit,
    onRequestNotifications: () -> Unit,
    onOpenSystemData: () -> Unit,
    onOpenUsageAccess: () -> Unit,
) {
    val used = snapshot.monthBytes ?: 0L
    val limit = (settings.dataLimitGb * 1024 * 1024 * 1024L).toLong().coerceAtLeast(1)
    val remaining = (limit - used).coerceAtLeast(0)
    val percent = ((used * 100.0 / limit).coerceIn(0.0, 999.0)).roundToInt()
    var limitValue by remember(settings.dataLimitGb) { mutableFloatStateOf(settings.dataLimitGb.coerceIn(1f, 500f)) }
    var cycle by remember(settings.cycleResetDay) { mutableFloatStateOf(settings.cycleResetDay.toFloat()) }
    FeaturePage("Data usage", "Allowance, history and alerts", onBack) {
        FeatureCard {
            Text("$percent% of allowance", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            LinearProgressIndicator(progress = { (used.toFloat() / limit).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().padding(vertical = 9.dp))
            FeatureLine("Today", feature.todayBytes?.let(::featureBytes) ?: "Unavailable")
            FeatureLine("Last 7 days", feature.sevenDayBytes?.let(::featureBytes) ?: "Unavailable")
            FeatureLine("This cycle", snapshot.monthBytes?.let(::featureBytes) ?: "Unavailable")
            FeatureLine("Remaining", featureBytes(remaining))
            FeatureLine("Last 3 days", snapshot.threeDayBytes?.let(::featureBytes) ?: "Unavailable")
        }
        FeatureCard {
            Text("Monthly allowance: ${limitValue.roundToInt()} GB", fontWeight = FontWeight.Bold)
            Slider(limitValue, { limitValue = it; onLimitChanged(it) }, valueRange = 1f..500f)
            Text("Cycle reset day: ${cycle.roundToInt()}", fontWeight = FontWeight.Bold)
            Slider(cycle, { cycle = it; onCycleDayChanged(it.roundToInt().coerceIn(1, 28)) }, valueRange = 1f..28f, steps = 26)
        }
        FeatureCard {
            SettingRow("Data alerts", "Notify at 50%, 75%, 90% and 100%", settings.dataAlertsEnabled, onAlertsChanged)
            FeatureLine("Notification permission", if (notificationsGranted) "Granted" else "Not granted")
            if (!notificationsGranted) FeatureButton("Allow notifications", Icons.Rounded.Notifications, onRequestNotifications)
            Spacer(Modifier.height(8.dp))
            FeatureButton("Android data usage", Icons.Rounded.OpenInNew, onOpenSystemData)
            if (!snapshot.usageAccessGranted) {
                Spacer(Modifier.height(8.dp))
                FeatureButton("Grant Usage Access", Icons.Rounded.DataUsage, onOpenUsageAccess)
            }
        }
    }
}

@Composable
fun SignalHistoryScreen(samples: List<SignalSample>, onBack: () -> Unit) {
    var windowMinutes by remember { mutableIntStateOf(15) }
    val now = System.currentTimeMillis()
    val visible = samples.filter { it.timeMs >= now - windowMinutes * 60_000L }
    FeaturePage("Signal history", "Find the best place for your pocket Wi-Fi", onBack) {
        FeatureCard {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(5, 15, 60).forEach { minutes ->
                    FilterChip(selected = windowMinutes == minutes, onClick = { windowMinutes = minutes }, label = { Text("${minutes}m") })
                }
            }
            Spacer(Modifier.height(12.dp))
            if (visible.size < 2) {
                Box(Modifier.fillMaxWidth().height(190.dp), contentAlignment = Alignment.Center) { Text("Collecting signal samples…", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                val lineColor = MaterialTheme.colorScheme.primary
                val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = .18f)
                Canvas(Modifier.fillMaxWidth().height(210.dp)) {
                    val minDbm = -125f
                    val maxDbm = -50f
                    repeat(4) { index ->
                        val y = size.height * index / 3f
                        drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                    }
                    val points = visible.mapIndexed { index, sample ->
                        val x = if (visible.size == 1) 0f else size.width * index / (visible.size - 1f)
                        val normalized = ((sample.dbm - minDbm) / (maxDbm - minDbm)).coerceIn(0f, 1f)
                        Offset(x, size.height * (1f - normalized))
                    }
                    points.zipWithNext().forEach { (a, b) -> drawLine(lineColor, a, b, strokeWidth = 5f, cap = StrokeCap.Round) }
                }
            }
            Spacer(Modifier.height(6.dp))
            FeatureLine("Current", visible.lastOrNull()?.let { "${it.dbm} dBm" } ?: "Unavailable")
            FeatureLine("Best", visible.maxOfOrNull { it.dbm }?.let { "$it dBm" } ?: "Unavailable")
            FeatureLine("Worst", visible.minOfOrNull { it.dbm }?.let { "$it dBm" } ?: "Unavailable")
        }
    }
}

@Composable
fun SpeedTestScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var running by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<SpeedTestResult?>(null) }
    FeaturePage("Speed test", "Small manual Internet performance test", onBack) {
        FeatureCard {
            Text("Quick test", fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
            Text("Uses roughly 2 MB download + 512 KB upload. Run it only when you want to test the current mobile connection.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricBox(Modifier.weight(1f), "Ping", result?.latencyMs?.let { "$it ms" } ?: "—", "Latency")
                MetricBox(Modifier.weight(1f), "Download", result?.downloadMbps?.let { String.format(Locale.US, "%.1f", it) } ?: "—", "Mbps")
                MetricBox(Modifier.weight(1f), "Upload", result?.uploadMbps?.let { String.format(Locale.US, "%.1f", it) } ?: "—", "Mbps")
            }
            result?.error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 9.sp, modifier = Modifier.padding(top = 8.dp)) }
            Spacer(Modifier.height(12.dp))
            Button(enabled = !running, onClick = {
                running = true
                result = null
                scope.launch {
                    result = SpeedTestEngine.run()
                    running = false
                }
            }, modifier = Modifier.fillMaxWidth()) {
                if (running) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Icon(Icons.Rounded.Speed, null)
                Spacer(Modifier.width(8.dp)); Text(if (running) "Testing…" else "Start speed test")
            }
        }
    }
}

@Composable
fun NetworkToolsScreen(
    feature: RouterFeatureSnapshot,
    onBack: () -> Unit,
    onMobile: () -> Unit,
    onSim: () -> Unit,
    onApn: () -> Unit,
    onVpn: () -> Unit,
    onDns: () -> Unit,
    onBattery: () -> Unit,
) {
    FeaturePage("Network tools", "Android network and power shortcuts", onBack) {
        FeatureCard {
            FeatureLine("Local IP", feature.localIp)
            FeatureLine("Gateway", feature.gatewayIp)
            FeatureLine("VPN", if (feature.vpnActive) "Active" else "Off")
            FeatureLine("Link estimate ↓", feature.downstreamKbps?.let { "${it / 1000.0} Mbps" } ?: "Unavailable")
            FeatureLine("Link estimate ↑", feature.upstreamKbps?.let { "${it / 1000.0} Mbps" } ?: "Unavailable")
        }
        FeatureGrid(
            listOf(
                FeatureAction(Icons.Rounded.SignalCellularAlt, "Mobile network", "Preferred network") { onMobile() },
                FeatureAction(Icons.Rounded.SimCard, "SIM", "SIM controls") { onSim() },
                FeatureAction(Icons.Rounded.SettingsEthernet, "APN", "Access points") { onApn() },
                FeatureAction(Icons.Rounded.VpnKey, "VPN", "VPN settings") { onVpn() },
                FeatureAction(Icons.Rounded.Dns, "Private DNS", "DNS settings") { onDns() },
                FeatureAction(Icons.Rounded.BatterySaver, "Battery", "Optimization") { onBattery() },
            )
        )
    }
}

@Composable
fun BackupRestoreScreen(
    backupJson: String,
    onImport: (String) -> Boolean,
    onShare: (String) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var status by remember { mutableStateOf<String?>(null) }
    FeaturePage("Backup & restore", "Portable ZeroFi settings JSON", onBack) {
        FeatureCard {
            Text("Backup includes your hotspot profile, including its password. Store it somewhere private.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
            Spacer(Modifier.height(10.dp))
            FeatureButton("Share backup JSON", Icons.Rounded.Share) { onShare(backupJson) }
            Spacer(Modifier.height(8.dp))
            FeatureButton("Copy backup to clipboard", Icons.Rounded.ContentCopy) {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("ZeroFi backup", backupJson))
                status = "Backup copied"
            }
            Spacer(Modifier.height(8.dp))
            FeatureButton("Restore from clipboard", Icons.Rounded.Restore) {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val raw = clipboard.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
                status = if (raw.isNotBlank() && onImport(raw)) "Backup restored. Reopen settings to refresh all values." else "Clipboard does not contain a valid ZeroFi backup."
            }
            status?.let { Text(it, color = MaterialTheme.colorScheme.primary, fontSize = 9.sp, modifier = Modifier.padding(top = 10.dp)) }
        }
    }
}

@Composable
fun StandbyScreenV2(
    snapshot: SystemSnapshot,
    hotspotActive: Boolean?,
    clients: HotspotClientSnapshot,
    usedBytes: Long?,
    limitGb: Float,
    oled: Boolean,
    onWake: () -> Unit,
) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); kotlinx.coroutines.delay(1_000L) } }
    val time = remember(now) { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(now)) }
    val date = remember(now) { SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date(now)) }
    val bg = if (oled) Color.Black else MaterialTheme.colorScheme.background
    Column(
        Modifier.fillMaxSize().background(bg).clickable(onClick = onWake).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(time, fontSize = 64.sp, fontWeight = FontWeight.Light, color = MaterialTheme.colorScheme.onBackground)
        Text(date, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(28.dp))
        Text(snapshot.networkType, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
        Text(snapshot.signalDbm?.let { "$it dBm" } ?: snapshot.carrierName, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(22.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StandbyPill(Icons.Rounded.Wifi, if (hotspotActive == true) "Hotspot ON" else "Hotspot OFF")
            StandbyPill(Icons.Rounded.Devices, if (clients.available) "${clients.clients.size} clients" else "Clients —")
            StandbyPill(Icons.Rounded.BatteryFull, snapshot.batteryPercent?.let { "$it%" } ?: "—")
        }
        Spacer(Modifier.height(14.dp))
        val limit = (limitGb * 1024 * 1024 * 1024L).toLong()
        Text("${usedBytes?.let(::featureBytes) ?: "—"} used · ${usedBytes?.let { featureBytes((limit - it).coerceAtLeast(0)) } ?: "—"} remaining", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        Text("Tap to open ZeroFi", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun DiagnosticsScreenV2(
    snapshot: SystemSnapshot,
    feature: RouterFeatureSnapshot,
    hotspotActive: Boolean?,
    clients: HotspotClientSnapshot,
    hotspotBand: String,
    onBack: () -> Unit,
) {
    val diagnosis = when {
        hotspotActive != true -> "Hotspot is not active. Turn it on before connecting other devices."
        !snapshot.cellularDefault -> "The phone is not currently using cellular as its default network."
        !snapshot.validatedInternet -> "Mobile connectivity exists, but Android does not report validated Internet access."
        snapshot.batteryTempC != null && snapshot.batteryTempC >= 45f -> "The battery is running hot. Improve airflow or reduce load."
        clients.available && clients.clients.isEmpty() -> "Internet looks healthy, but no hotspot clients are currently detected."
        else -> "ZeroFi's core router checks look healthy."
    }
    FeaturePage("Router diagnostics", "Why isn't Internet working?", onBack) {
        FeatureCard {
            Text(diagnosis, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
        FeatureCard {
            HealthLine("Hotspot", hotspotActive == true, if (hotspotActive == true) "Active" else "Off / unknown")
            HealthLine("Cellular default", snapshot.cellularDefault, if (snapshot.cellularDefault) snapshot.networkType else "No")
            HealthLine("Validated Internet", snapshot.validatedInternet, if (snapshot.validatedInternet) "Yes" else "No")
            HealthLine("Client visibility", clients.available, if (clients.available) "${clients.clients.size} clients" else "Android restricted")
            HealthLine("Usage Access", snapshot.usageAccessGranted, if (snapshot.usageAccessGranted) "Granted" else "Missing")
            HealthLine("Phone status", snapshot.phonePermissionGranted, if (snapshot.phonePermissionGranted) "Granted" else "Missing")
        }
        FeatureCard {
            FeatureLine("Carrier", snapshot.carrierName)
            FeatureLine("Network", snapshot.networkType)
            FeatureLine("Signal", snapshot.signalDbm?.let { "$it dBm" } ?: "Unavailable")
            FeatureLine("Band", hotspotBand)
            FeatureLine("Battery", snapshot.batteryPercent?.let { "$it%" } ?: "Unavailable")
            FeatureLine("Temperature", snapshot.batteryTempC?.let { "%.1f°C".format(it) } ?: "Unavailable")
            FeatureLine("Uptime", snapshot.uptimeText)
            FeatureLine("Local IP", feature.localIp)
            FeatureLine("Gateway", feature.gatewayIp)
            FeatureLine("Device", feature.deviceName)
            FeatureLine("OS", feature.androidVersion)
        }
    }
}

private data class FeatureAction(val icon: ImageVector, val title: String, val subtitle: String, val onClick: () -> Unit)

@Composable private fun FeaturePage(title: String, subtitle: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Back") }
            Column { Text(title, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold); Text(subtitle, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        Spacer(Modifier.height(8.dp))
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

@Composable private fun FeatureCard(content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, shape).border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .14f), shape).padding(14.dp), content = content)
}

@Composable private fun FeatureGrid(actions: List<FeatureAction>) {
    actions.chunked(2).forEach { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            row.forEach { action ->
                Row(
                    Modifier.weight(1f).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(15.dp)).border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .14f), RoundedCornerShape(15.dp)).clickable(onClick = action.onClick).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FeatureCircle(action.icon, MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) { Text(action.title, fontWeight = FontWeight.Bold, fontSize = 10.sp); Text(action.subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 7.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                }
            }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable private fun FeatureCircle(icon: ImageVector, color: Color) {
    Box(Modifier.size(34.dp).background(color.copy(alpha = .14f), CircleShape), contentAlignment = Alignment.Center) { Icon(icon, null, tint = color, modifier = Modifier.size(18.dp)) }
}

@Composable private fun MetricBox(modifier: Modifier, label: String, value: String, detail: String) {
    Column(modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .45f), RoundedCornerShape(13.dp)).padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 7.sp)
        Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 6.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable private fun FeatureLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) { Text(label, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp); Text(value, fontWeight = FontWeight.SemiBold, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
}

@Composable private fun HealthLine(label: String, healthy: Boolean, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(9.dp).background(if (healthy) Color(0xFF35D97B) else MaterialTheme.colorScheme.error, CircleShape)); Spacer(Modifier.width(8.dp)); Text(label, Modifier.weight(1f), fontSize = 9.sp); Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
    }
}

@Composable private fun FeatureButton(text: String, icon: ImageVector, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) { Icon(icon, null, Modifier.size(17.dp)); Spacer(Modifier.width(7.dp)); Text(text) }
}

@Composable private fun SettingRow(title: String, subtitle: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.SemiBold, fontSize = 11.sp); Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp) }
        Switch(checked, onChecked)
    }
}

@Composable private fun StandbyPill(icon: ImageVector, text: String) {
    Row(Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .55f), RoundedCornerShape(18.dp)).padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, Modifier.size(15.dp), tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(5.dp)); Text(text, fontSize = 8.sp, fontWeight = FontWeight.SemiBold) }
}

private fun featureBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format(Locale.US, "%.0f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format(Locale.US, "%.1f MB", mb)
    return String.format(Locale.US, "%.2f GB", mb / 1024.0)
}
private fun featureRate(bytes: Long): String {
    if (bytes < 1024) return "$bytes B/s"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format(Locale.US, "%.0f KB/s", kb)
    return String.format(Locale.US, "%.1f MB/s", kb / 1024.0)
}
