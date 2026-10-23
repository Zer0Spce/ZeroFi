package com.zer0spce.zerofi.ui

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zer0spce.zerofi.platform.SystemSnapshot
import com.zer0spce.zerofi.ui.theme.AccentTheme
import java.util.Locale

@Composable
fun DashboardScreen(
    snapshot: SystemSnapshot,
    hotspotActive: Boolean?,
    hotspotBand: String,
    dataLimitGb: Float,
    accentTheme: AccentTheme,
    darkMode: Boolean,
    oledSaver: Boolean,
    onAccentThemeChange: (AccentTheme) -> Unit,
    onDarkModeChange: (Boolean) -> Unit,
    onNavigate: (AppScreen) -> Unit,
    onRequestPhonePermission: () -> Unit,
    onRequestUsageAccess: () -> Unit,
    lockRunning: Boolean,
    onRouterLockToggle: () -> Unit,
) {
    val monthlyLimitBytes = (dataLimitGb * GIB).toLong().coerceAtLeast(1L)
    val monthProgress = snapshot.monthBytes?.let { (it.toFloat() / monthlyLimitBytes).coerceIn(0f, 1f) }
    val threeDayTarget = (monthlyLimitBytes * 3L / 30L).coerceAtLeast(1L)
    val threeDayProgress = snapshot.threeDayBytes?.let { (it.toFloat() / threeDayTarget).coerceIn(0f, 1f) }
    val signalProgress = snapshot.signalLevel?.let { it.coerceIn(0, 4) / 4f }

    BoxWithConstraints(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val portrait = maxHeight > maxWidth
        Column(Modifier.fillMaxSize().padding(horizontal = if (portrait) 16.dp else 22.dp, vertical = 12.dp)) {
            ScrHeader(snapshot, portrait, darkMode, lockRunning, { onDarkModeChange(!darkMode) }, onRouterLockToggle) { onNavigate(AppScreen.SETTINGS) }
            Spacer(Modifier.height(10.dp))
            if (portrait) PortraitDashboard(snapshot, hotspotActive, hotspotBand, monthlyLimitBytes, monthProgress, threeDayProgress, signalProgress, onNavigate, onRequestPhonePermission, onRequestUsageAccess)
            else LandscapeDashboard(snapshot, hotspotActive, hotspotBand, monthlyLimitBytes, monthProgress, threeDayProgress, signalProgress, onNavigate, onRequestPhonePermission, onRequestUsageAccess)
        }
    }
}

@Composable
private fun ScrHeader(snapshot: SystemSnapshot, portrait: Boolean, darkMode: Boolean, locked: Boolean, onTheme: () -> Unit, onLock: () -> Unit, onSettings: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column { Text("ZeroFi", color = MaterialTheme.colorScheme.onBackground, fontSize = 24.sp, fontWeight = FontWeight.Bold); Text("Mobile Wi-Fi", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp) }
        Spacer(Modifier.weight(1f))
        if (!portrait) { StatusPill(Icons.Rounded.SignalCellularAlt, if (snapshot.validatedInternet) "Online" else "Offline"); Spacer(Modifier.width(6.dp)) }
        StatusPill(Icons.Rounded.BatteryFull, snapshot.batteryPercent?.let { "$it%" } ?: "—")
        Spacer(Modifier.width(6.dp)); HeaderButton(if (darkMode) Icons.Rounded.LightMode else Icons.Rounded.DarkMode, onTheme)
        Spacer(Modifier.width(5.dp)); HeaderButton(if (locked) Icons.Rounded.Lock else Icons.Rounded.LockOpen, onLock)
        Spacer(Modifier.width(5.dp)); HeaderButton(Icons.Rounded.Settings, onSettings)
    }
}

@Composable
private fun PortraitDashboard(snapshot: SystemSnapshot, hotspotActive: Boolean?, hotspotBand: String, monthlyLimitBytes: Long, monthProgress: Float?, threeDayProgress: Float?, signalProgress: Float?, onNavigate: (AppScreen) -> Unit, onPhone: () -> Unit, onUsage: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
        NetworkHero(snapshot, signalProgress, 148.dp, if (snapshot.phonePermissionGranted) null else onPhone); Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            UsageMeter(Modifier.weight(1f), "This month", if (snapshot.usageAccessGranted) snapshot.monthBytes?.let(::dashboardFormatBytes) ?: "0 B" else "Enable", if (snapshot.usageAccessGranted) "of ${dashboardFormatBytes(monthlyLimitBytes)}" else "Tap for usage access", monthProgress, 92.dp, if (snapshot.usageAccessGranted) null else onUsage)
            UsageMeter(Modifier.weight(1f), "Last 3 days", if (snapshot.usageAccessGranted) snapshot.threeDayBytes?.let(::dashboardFormatBytes) ?: "0 B" else "Enable", if (snapshot.usageAccessGranted) "Mobile data" else "Tap for usage access", threeDayProgress, 92.dp, if (snapshot.usageAccessGranted) null else onUsage)
        }
        Spacer(Modifier.height(12.dp)); QuickGrid(hotspotActive, hotspotBand, onNavigate); Spacer(Modifier.height(10.dp)); DeviceCard(snapshot, hotspotActive) { onNavigate(AppScreen.DEVICES) }; Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun LandscapeDashboard(snapshot: SystemSnapshot, hotspotActive: Boolean?, hotspotBand: String, monthlyLimitBytes: Long, monthProgress: Float?, threeDayProgress: Float?, signalProgress: Float?, onNavigate: (AppScreen) -> Unit, onPhone: () -> Unit, onUsage: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.weight(1.05f), horizontalArrangement = Arrangement.SpaceEvenly) {
                UsageMeter(Modifier.width(108.dp), "This month", if (snapshot.usageAccessGranted) snapshot.monthBytes?.let(::dashboardFormatBytes) ?: "0 B" else "Enable", if (snapshot.usageAccessGranted) "of ${dashboardFormatBytes(monthlyLimitBytes)}" else "Tap to allow", monthProgress, 88.dp, if (snapshot.usageAccessGranted) null else onUsage)
                UsageMeter(Modifier.width(108.dp), "Last 3 days", if (snapshot.usageAccessGranted) snapshot.threeDayBytes?.let(::dashboardFormatBytes) ?: "0 B" else "Enable", if (snapshot.usageAccessGranted) "Mobile data" else "Tap to allow", threeDayProgress, 88.dp, if (snapshot.usageAccessGranted) null else onUsage)
            }
            Box(Modifier.weight(.75f), contentAlignment = Alignment.Center) { NetworkHero(snapshot, signalProgress, 138.dp, if (snapshot.phonePermissionGranted) null else onPhone) }
            Box(Modifier.weight(1.15f)) { QuickGrid(hotspotActive, hotspotBand, onNavigate) }
        }
        Spacer(Modifier.height(10.dp)); DeviceCard(snapshot, hotspotActive) { onNavigate(AppScreen.DEVICES) }
    }
}

@Composable
private fun NetworkHero(snapshot: SystemSnapshot, progress: Float?, size: Dp, onClick: (() -> Unit)?) {
    Column((if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Mobile network", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp); Spacer(Modifier.height(4.dp))
        Box(Modifier.size(size), contentAlignment = Alignment.Center) { Ring(progress, size); Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Rounded.SignalCellularAlt, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp)); Text(if (!snapshot.phonePermissionGranted) "Phone status" else snapshot.networkType.ifBlank { "Network" }, color = MaterialTheme.colorScheme.onBackground, fontSize = if (!snapshot.phonePermissionGranted) 14.sp else 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center); Text(if (!snapshot.phonePermissionGranted) "Tap to allow" else snapshot.signalDbm?.let { "$it dBm" } ?: snapshot.carrierName.ifBlank { "Signal unavailable" }, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp, textAlign = TextAlign.Center) } }
    }
}

@Composable
private fun UsageMeter(modifier: Modifier, label: String, value: String, detail: String, progress: Float?, size: Dp, onClick: (() -> Unit)?) {
    Column(modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier), horizontalAlignment = Alignment.CenterHorizontally) { Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp); Spacer(Modifier.height(3.dp)); Box(Modifier.size(size), contentAlignment = Alignment.Center) { Ring(progress, size); Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(value, color = MaterialTheme.colorScheme.onBackground, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 7.sp, textAlign = TextAlign.Center, maxLines = 2) } } }
}

@Composable
private fun Ring(progress: Float?, size: Dp) {
    val animated by animateFloatAsState(progress?.coerceIn(0f, 1f) ?: 0f, label = "ring")
    val accent = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.outline.copy(alpha = .18f)
    Canvas(Modifier.size(size)) { val stroke = Stroke(size.toPx() * .05f, cap = StrokeCap.Round); drawArc(track, -90f, 360f, false, style = stroke); drawArc(if (progress == null) accent.copy(alpha = .35f) else accent, -90f, if (progress == null) 32f else animated * 360f, false, style = stroke) }
}

@Composable
private fun QuickGrid(hotspotActive: Boolean?, hotspotBand: String, onNavigate: (AppScreen) -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { QuickTile(Modifier.weight(1f), Icons.Rounded.Wifi, "Wi-Fi", when (hotspotActive) { true -> "On"; false -> "Off"; null -> "Hotspot" }) { onNavigate(AppScreen.HOTSPOT) }; QuickTile(Modifier.weight(1f), Icons.Rounded.SwapVert, "Network", hotspotBand.ifBlank { "System" }) { onNavigate(AppScreen.DATA) } }; Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { QuickTile(Modifier.weight(1f), Icons.Rounded.PowerSettingsNew, "Power saving", "Battery") { onNavigate(AppScreen.SETTINGS) }; QuickTile(Modifier.weight(1f), Icons.Rounded.Brightness6, "Display", "Screen") { onNavigate(AppScreen.DISPLAY) } } }
}

@Composable
private fun QuickTile(modifier: Modifier, icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) { val shape = RoundedCornerShape(16.dp); Row(modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .55f), shape).border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .12f), shape).clickable(onClick = onClick).padding(11.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(34.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = .12f), CircleShape), contentAlignment = Alignment.Center) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) }; Spacer(Modifier.width(9.dp)); Column { Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp, fontWeight = FontWeight.SemiBold); Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp, maxLines = 1) } } }

@Composable
private fun DeviceCard(snapshot: SystemSnapshot, hotspotActive: Boolean?, onClick: () -> Unit) { val shape = RoundedCornerShape(16.dp); Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .42f), shape).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Rounded.Devices, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(21.dp)); Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text("Connected devices", color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp, fontWeight = FontWeight.SemiBold); Text(when (hotspotActive) { true -> "Hotspot active · tap for clients"; false -> "Hotspot is off"; null -> "Tap for client details" }, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp) }; Column(horizontalAlignment = Alignment.End) { Text("↓ ${snapshot.downloadBytesPerSecond?.let(::dashboardFormatRate) ?: "—"}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp); Text("↑ ${snapshot.uploadBytesPerSecond?.let(::dashboardFormatRate) ?: "—"}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp) } } }

@Composable
private fun StatusPill(icon: ImageVector, text: String) { Row(Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .55f), RoundedCornerShape(14.dp)).padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(13.dp)); Text(text, color = MaterialTheme.colorScheme.onSurface, fontSize = 8.sp) } }

@Composable
private fun HeaderButton(icon: ImageVector, onClick: () -> Unit) { Box(Modifier.size(32.dp).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .55f), CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) { Icon(icon, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp)) } }

private fun dashboardFormatBytes(bytes: Long): String { if (bytes < 1024) return "$bytes B"; val kb = bytes / 1024.0; if (kb < 1024) return String.format(Locale.US, "%.0f KB", kb); val mb = kb / 1024.0; if (mb < 1024) return String.format(Locale.US, "%.1f MB", mb); return String.format(Locale.US, "%.2f GB", mb / 1024.0) }
private fun dashboardFormatRate(bytes: Long): String { if (bytes < 1024) return "$bytes B/s"; val kb = bytes / 1024.0; if (kb < 1024) return String.format(Locale.US, "%.0f KB/s", kb); return String.format(Locale.US, "%.1f MB/s", kb / 1024.0) }
private const val GIB = 1024L * 1024L * 1024L