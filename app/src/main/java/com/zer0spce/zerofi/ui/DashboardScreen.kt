package com.zer0spce.zerofi.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
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
    val accent = MaterialTheme.colorScheme.primary
    val background = when {
        oledSaver -> Brush.linearGradient(listOf(Color.Black, Color.Black))
        darkMode -> Brush.verticalGradient(listOf(MaterialTheme.colorScheme.background, accent.copy(alpha = .035f), MaterialTheme.colorScheme.background))
        else -> Brush.verticalGradient(listOf(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .28f), MaterialTheme.colorScheme.background))
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(background)) {
        val portrait = maxHeight > maxWidth
        val monthlyLimitBytes = (dataLimitGb * GIB).toLong().coerceAtLeast(1L)
        val monthProgress = snapshot.monthBytes?.let { (it.toFloat() / monthlyLimitBytes).coerceIn(0f, 1f) }
        val threeDayTarget = (monthlyLimitBytes * 3L / 30L).coerceAtLeast(1L)
        val threeDayProgress = snapshot.threeDayBytes?.let { (it.toFloat() / threeDayTarget).coerceIn(0f, 1f) }
        val signalProgress = snapshot.signalLevel?.let { it.coerceIn(0, 4) / 4f }

        Column(
            Modifier.fillMaxSize().padding(horizontal = if (portrait) 16.dp else 22.dp, vertical = 14.dp)
        ) {
            CleanHeader(
                snapshot, accentTheme, darkMode, portrait, lockRunning,
                onAccentThemeChange, onDarkModeChange, onRouterLockToggle,
                { onNavigate(AppScreen.SETTINGS) },
            )
            Spacer(Modifier.height(if (portrait) 12.dp else 8.dp))

            if (portrait) {
                PortraitDashboard(
                    snapshot, hotspotActive, hotspotBand, monthlyLimitBytes,
                    monthProgress, threeDayProgress, signalProgress,
                    onNavigate, onRequestPhonePermission, onRequestUsageAccess,
                )
            } else {
                LandscapeDashboard(
                    snapshot, hotspotActive, hotspotBand, monthlyLimitBytes,
                    monthProgress, threeDayProgress, signalProgress,
                    onNavigate, onRequestPhonePermission, onRequestUsageAccess,
                )
            }
        }
    }
}

@Composable
private fun CleanHeader(
    snapshot: SystemSnapshot,
    accentTheme: AccentTheme,
    darkMode: Boolean,
    portrait: Boolean,
    lockRunning: Boolean,
    onAccentThemeChange: (AccentTheme) -> Unit,
    onDarkModeChange: (Boolean) -> Unit,
    onRouterLockToggle: () -> Unit,
    onSettings: () -> Unit,
) {
    var themeMenuOpen by remember { mutableStateOf(false) }
    Column {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("ZeroFi", fontSize = if (portrait) 27.sp else 26.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-.5).sp)
                Text("MOBILE WI-FI", color = MaterialTheme.colorScheme.primary, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp)
            }
            Spacer(Modifier.weight(1f))
            if (!portrait) {
                StatusPill(Icons.Rounded.SignalCellularAlt, if (snapshot.validatedInternet) "Online" else "Offline")
                Spacer(Modifier.width(6.dp))
            }
            StatusPill(
                Icons.Rounded.BatteryFull,
                buildString {
                    append(snapshot.batteryPercent?.let { "$it%" } ?: "—")
                    if (!portrait && snapshot.batteryTimeRemainingText.isNotBlank()) append(" · ${snapshot.batteryTimeRemainingText}")
                },
            )
            Spacer(Modifier.width(6.dp))
            Box {
                CircleHeaderButton(Icons.Rounded.DarkMode, accentTheme.accent) { themeMenuOpen = true }
                DropdownMenu(themeMenuOpen, { themeMenuOpen = false }) {
                    AccentTheme.entries.forEach { theme ->
                        DropdownMenuItem(
                            text = { Text(theme.label) },
                            leadingIcon = { Box(Modifier.size(11.dp).background(theme.accent, CircleShape)) },
                            onClick = { onAccentThemeChange(theme); themeMenuOpen = false },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(if (darkMode) "Use light mode" else "Use dark mode") },
                        onClick = { onDarkModeChange(!darkMode); themeMenuOpen = false },
                    )
                }
            }
            Spacer(Modifier.width(5.dp))
            CircleHeaderButton(Icons.Rounded.Settings, MaterialTheme.colorScheme.onSurfaceVariant, onSettings)
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            LockButton(lockRunning, onRouterLockToggle)
        }
    }
}

@Composable
private fun PortraitDashboard(
    snapshot: SystemSnapshot,
    hotspotActive: Boolean?,
    hotspotBand: String,
    monthlyLimitBytes: Long,
    monthProgress: Float?,
    threeDayProgress: Float?,
    signalProgress: Float?,
    onNavigate: (AppScreen) -> Unit,
    onRequestPhonePermission: () -> Unit,
    onRequestUsageAccess: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Full-width center container prevents the signal meter from drifting left.
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            SignalHero(
                snapshot, signalProgress, 180.dp,
                if (snapshot.phonePermissionGranted) null else onRequestPhonePermission,
            )
        }
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            DataStatus(
                Modifier.weight(1f), "This month",
                snapshot.monthBytes?.let(::formatDashboardBytes) ?: "—",
                if (snapshot.usageAccessGranted) "of ${formatDashboardBytes(monthlyLimitBytes)}" else "Tap to allow access",
                monthProgress, 108.dp,
                if (snapshot.usageAccessGranted) null else onRequestUsageAccess,
            )
            DataStatus(
                Modifier.weight(1f), "Last 3 days",
                snapshot.threeDayBytes?.let(::formatDashboardBytes) ?: "—",
                if (snapshot.usageAccessGranted) "Mobile data" else "Tap to allow access",
                threeDayProgress, 108.dp,
                if (snapshot.usageAccessGranted) null else onRequestUsageAccess,
            )
        }
        SectionDivider()
        UtilityGrid(hotspotActive, hotspotBand, onNavigate)
        SectionDivider()
        DeviceFooter(snapshot, hotspotActive) { onNavigate(AppScreen.DEVICES) }
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun LandscapeDashboard(
    snapshot: SystemSnapshot,
    hotspotActive: Boolean?,
    hotspotBand: String,
    monthlyLimitBytes: Long,
    monthProgress: Float?,
    threeDayProgress: Float?,
    signalProgress: Float?,
    onNavigate: (AppScreen) -> Unit,
    onRequestPhonePermission: () -> Unit,
    onRequestUsageAccess: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.weight(1f).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Both usage meters get identical fixed square space so neither can collapse into an oval.
            Row(
                Modifier.weight(1.05f),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DataStatus(
                    Modifier.width(122.dp), "This month",
                    snapshot.monthBytes?.let(::formatDashboardBytes) ?: "—",
                    if (snapshot.usageAccessGranted) "of ${formatDashboardBytes(monthlyLimitBytes)}" else "Usage access needed",
                    monthProgress, 104.dp,
                    if (snapshot.usageAccessGranted) null else onRequestUsageAccess,
                )
                DataStatus(
                    Modifier.width(122.dp), "Last 3 days",
                    snapshot.threeDayBytes?.let(::formatDashboardBytes) ?: "—",
                    if (snapshot.usageAccessGranted) "Mobile data" else "Usage access needed",
                    threeDayProgress, 104.dp,
                    if (snapshot.usageAccessGranted) null else onRequestUsageAccess,
                )
            }
            Box(Modifier.weight(.9f), contentAlignment = Alignment.Center) {
                SignalHero(
                    snapshot, signalProgress, 170.dp,
                    if (snapshot.phonePermissionGranted) null else onRequestPhonePermission,
                )
            }
            Box(Modifier.weight(1.2f), contentAlignment = Alignment.Center) {
                UtilityGrid(hotspotActive, hotspotBand, onNavigate)
            }
        }
        SectionDivider(compact = true)
        DeviceFooter(snapshot, hotspotActive) { onNavigate(AppScreen.DEVICES) }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun UtilityGrid(hotspotActive: Boolean?, hotspotBand: String, onNavigate: (AppScreen) -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            UtilityTile(Modifier.weight(1f), Icons.Rounded.Wifi, "Wi-Fi", when (hotspotActive) { true -> "Hotspot on"; false -> "Hotspot off"; null -> "Hotspot" }) { onNavigate(AppScreen.HOTSPOT) }
            UtilityTile(Modifier.weight(1f), Icons.Rounded.SwapVert, "Network", hotspotBand.ifBlank { "System" }) { onNavigate(AppScreen.DATA) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            UtilityTile(Modifier.weight(1f), Icons.Rounded.PowerSettingsNew, "Power saving", "Battery controls") { onNavigate(AppScreen.SETTINGS) }
            UtilityTile(Modifier.weight(1f), Icons.Rounded.Brightness6, "Display", "Screen controls") { onNavigate(AppScreen.DISPLAY) }
        }
    }
}

@Composable
private fun SignalHero(snapshot: SystemSnapshot, progress: Float?, size: Dp, onClick: (() -> Unit)?) {
    val click = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Column(click, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Mobile network", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(6.dp))
        Box(Modifier.size(size), contentAlignment = Alignment.Center) {
            RingMeter(progress, size, .052f)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.SignalCellularAlt, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(23.dp))
                Spacer(Modifier.height(3.dp))
                Text(snapshot.networkType.ifBlank { "—" }, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                Text(
                    when {
                        !snapshot.phonePermissionGranted -> "Tap to allow phone status"
                        snapshot.signalDbm != null -> "${snapshot.signalDbm} dBm"
                        snapshot.signalLevel != null -> "Signal ${snapshot.signalLevel}/4"
                        snapshot.carrierName.isNotBlank() -> snapshot.carrierName
                        else -> "Signal unavailable"
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, textAlign = TextAlign.Center,
                )
            }
        }
        if (snapshot.carrierName.isNotBlank() && snapshot.phonePermissionGranted) {
            Spacer(Modifier.height(3.dp))
            Text(snapshot.carrierName, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun DataStatus(modifier: Modifier, label: String, value: String, detail: String, progress: Float?, size: Dp, onClick: (() -> Unit)?) {
    val click = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Column(modifier.then(click), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(4.dp))
        Box(Modifier.size(size), contentAlignment = Alignment.Center) {
            RingMeter(progress, size, .047f)
            Column(Modifier.padding(horizontal = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 7.sp, lineHeight = 9.sp, textAlign = TextAlign.Center, maxLines = 2)
            }
        }
    }
}

@Composable
private fun RingMeter(progress: Float?, size: Dp, strokeFraction: Float) {
    val animated by animateFloatAsState(progress?.coerceIn(0f, 1f) ?: 0f, label = "ring")
    val accent = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.outline.copy(alpha = .14f)
    Canvas(Modifier.size(size)) {
        val stroke = Stroke(size.toPx() * strokeFraction, cap = StrokeCap.Round)
        drawArc(track, -90f, 360f, false, style = stroke)
        drawArc(if (progress == null) accent.copy(alpha = .32f) else accent, -90f, if (progress == null) 34f else animated * 360f, false, style = stroke)
    }
}

@Composable
private fun UtilityTile(modifier: Modifier, icon: ImageVector, label: String, value: String, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) .975f else 1f, label = "tile")
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier.graphicsLayer { scaleX = scale; scaleY = scale }
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .30f), shape)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .14f), shape)
            .clickable(interaction, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(38.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = .10f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun DeviceFooter(snapshot: SystemSnapshot, hotspotActive: Boolean?, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = .09f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Devices, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text("Connected devices", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Text(when (hotspotActive) { true -> "Hotspot active · tap for client details"; false -> "Hotspot is off"; null -> "Tap for hotspot client details" }, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("↓ ${snapshot.downloadBytesPerSecond?.let(::formatRate) ?: "—"}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp)
            Text("↑ ${snapshot.uploadBytesPerSecond?.let(::formatRate) ?: "—"}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp)
        }
    }
}

@Composable
private fun LockButton(locked: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .24f), shape)
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .35f), shape)
            .clickable(onClick = onClick).padding(horizontal = 13.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(if (locked) Icons.Rounded.Lock else Icons.Rounded.LockOpen, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Text(if (locked) "UNLOCK" else "LOCK", fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StatusPill(icon: ImageVector, text: String) {
    Row(
        Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = .65f), RoundedCornerShape(14.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .16f), RoundedCornerShape(14.dp))
            .padding(horizontal = 9.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
        Text(text, fontSize = 9.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

@Composable
private fun CircleHeaderButton(icon: ImageVector, tint: Color, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) .94f else 1f, label = "header")
    Box(
        Modifier.size(34.dp).graphicsLayer { scaleX = scale; scaleY = scale }
            .background(MaterialTheme.colorScheme.surface.copy(alpha = .65f), CircleShape)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .16f), CircleShape)
            .clickable(interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = tint, modifier = Modifier.size(17.dp)) }
}

@Composable
private fun SectionDivider(compact: Boolean = false) {
    Spacer(Modifier.height(if (compact) 7.dp else 13.dp))
    Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = .10f)))
    Spacer(Modifier.height(if (compact) 7.dp else 13.dp))
}

private fun formatDashboardBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format(Locale.US, "%.0f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format(Locale.US, "%.1f MB", mb)
    return String.format(Locale.US, "%.2f GB", mb / 1024.0)
}

private fun formatRate(bytesPerSecond: Long): String {
    if (bytesPerSecond < 1024L) return "$bytesPerSecond B/s"
    val kb = bytesPerSecond / 1024.0
    if (kb < 1024.0) return String.format(Locale.US, "%.0f KB/s", kb)
    return String.format(Locale.US, "%.1f MB/s", kb / 1024.0)
}

private const val GIB = 1024L * 1024L * 1024L
