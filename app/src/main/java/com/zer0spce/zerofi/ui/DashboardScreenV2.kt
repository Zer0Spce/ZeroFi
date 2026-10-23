package com.zer0spce.zerofi.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

private val HomeCyan = Color(0xFF25C9D5)
private val HomeGreen = Color(0xFF35D97B)
private val HomeBlue = Color(0xFF4B9DFF)
private val HomeSlate = Color(0xFF607D9C)
private val HomeTeal = Color(0xFF20B99A)
private val HomePurple = Color(0xFF7568E8)

@Composable
fun DashboardScreenV2(
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
    val monthlyLimitBytes = (dataLimitGb * HOME_GIB).toLong().coerceAtLeast(1L)
    val monthProgress = snapshot.monthBytes?.let { (it.toFloat() / monthlyLimitBytes).coerceIn(0f, 1f) }
    val threeDayTarget = (monthlyLimitBytes * 3L / 30L).coerceAtLeast(1L)
    val threeDayProgress = snapshot.threeDayBytes?.let { (it.toFloat() / threeDayTarget).coerceIn(0f, 1f) }
    val signalProgress = snapshot.signalLevel?.let { it.coerceIn(0, 4) / 4f }

    BoxWithConstraints(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val portrait = maxHeight > maxWidth
        Column(
            Modifier.fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                .padding(start = if (portrait) 12.dp else 16.dp, end = if (portrait) 12.dp else 16.dp, top = 8.dp, bottom = 8.dp)
        ) {
            HomeHeader(
                snapshot,
                darkMode,
                lockRunning,
                { onNavigate(AppScreen.SETTINGS) },
                { onDarkModeChange(!darkMode) },
                onRouterLockToggle,
                { onNavigate(AppScreen.HOTSPOT) },
            )
            Spacer(Modifier.height(8.dp))
            if (portrait) {
                HomePortrait(
                    snapshot, hotspotActive, hotspotBand, monthlyLimitBytes,
                    monthProgress, threeDayProgress, signalProgress, oledSaver,
                    onNavigate, onRequestPhonePermission, onRequestUsageAccess,
                )
            } else {
                HomeLandscape(
                    snapshot, hotspotActive, hotspotBand, monthlyLimitBytes,
                    monthProgress, threeDayProgress, signalProgress, oledSaver,
                    onNavigate, onRequestPhonePermission, onRequestUsageAccess,
                )
            }
        }
    }
}

@Composable
private fun HomeHeader(
    snapshot: SystemSnapshot,
    darkMode: Boolean,
    locked: Boolean,
    onMenu: () -> Unit,
    onTheme: () -> Unit,
    onLock: () -> Unit,
    onQr: () -> Unit,
) {
    Box(Modifier.fillMaxWidth().height(42.dp), contentAlignment = Alignment.Center) {
        HomeHeaderIcon(Icons.Rounded.Menu, "Menu", onMenu, Modifier.align(Alignment.CenterStart))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("ZeroFi", color = MaterialTheme.colorScheme.onBackground, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            val batteryStatus = buildString {
                snapshot.batteryPercent?.let { append("$it% · ") }
                append(snapshot.batteryTimeRemainingText)
            }
            Text(batteryStatus, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 7.sp, maxLines = 1)
        }
        Row(
            Modifier.align(Alignment.CenterEnd),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                Modifier.height(30.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .62f), RoundedCornerShape(15.dp))
                    .clickable(onClick = onTheme)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    if (darkMode) Icons.Rounded.DarkMode else Icons.Rounded.LightMode,
                    null,
                    tint = if (darkMode) HomeBlue else HomeCyan,
                    modifier = Modifier.size(14.dp),
                )
                Text(if (darkMode) "Dark" else "Light", color = MaterialTheme.colorScheme.onSurface, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
            HomeHeaderIcon(if (locked) Icons.Rounded.Lock else Icons.Rounded.LockOpen, if (locked) "Unlock" else "Lock", onLock)
            HomeHeaderIcon(Icons.Rounded.QrCode2, "Wi-Fi QR", onQr)
        }
    }
}

@Composable
private fun HomePortrait(
    snapshot: SystemSnapshot,
    hotspotActive: Boolean?,
    hotspotBand: String,
    monthlyLimitBytes: Long,
    monthProgress: Float?,
    threeDayProgress: Float?,
    signalProgress: Float?,
    oledSaver: Boolean,
    onNavigate: (AppScreen) -> Unit,
    onPhone: () -> Unit,
    onUsage: () -> Unit,
) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HomeCard(Modifier.weight(1.10f).fillMaxWidth()) {
            Text("Data usage", color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Spacer(Modifier.height(4.dp))
            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                HomeUsageGauge(
                    "1 month",
                    "Current cycle",
                    if (snapshot.usageAccessGranted) snapshot.monthBytes?.let(::homeFormatBytes) ?: "0 B" else "Enable",
                    if (snapshot.usageAccessGranted) "/ ${homeFormatBytes(monthlyLimitBytes)}" else "Tap to allow",
                    monthProgress,
                    HomeCyan,
                    onClick = if (snapshot.usageAccessGranted) null else onUsage,
                )
                HomeUsageGauge(
                    "3 days",
                    "Recent usage",
                    if (snapshot.usageAccessGranted) snapshot.threeDayBytes?.let(::homeFormatBytes) ?: "0 B" else "Enable",
                    if (snapshot.usageAccessGranted) "Mobile data" else "Tap to allow",
                    threeDayProgress,
                    HomeGreen,
                    onClick = if (snapshot.usageAccessGranted) null else onUsage,
                )
            }
        }

        Row(Modifier.weight(.92f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HomeCard(Modifier.weight(1f).fillMaxHeight()) {
                HomeNetworkGauge(snapshot, signalProgress, if (snapshot.phonePermissionGranted) null else onPhone)
            }
            HomeCard(Modifier.weight(1f).fillMaxHeight().clickable { onNavigate(AppScreen.DEVICES) }) {
                HomeDevices(snapshot, hotspotActive)
            }
        }

        HomeActionGrid(Modifier.weight(.78f).fillMaxWidth(), hotspotActive, hotspotBand, oledSaver, onNavigate)

        HomeCard(Modifier.height(52.dp).fillMaxWidth().clickable { onNavigate(AppScreen.DIAGNOSTICS) }) {
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                HomeCircleIcon(Icons.Rounded.Info, HomeBlue)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text("Router status", color = MaterialTheme.colorScheme.onSurface, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "${snapshot.carrierName.ifBlank { "Carrier" }} · ${snapshot.networkType.ifBlank { "Network" }} · ${hotspotBand.ifBlank { "System" }}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 7.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    if (snapshot.validatedInternet) "ONLINE" else "OFFLINE",
                    color = if (snapshot.validatedInternet) HomeGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun HomeLandscape(
    snapshot: SystemSnapshot,
    hotspotActive: Boolean?,
    hotspotBand: String,
    monthlyLimitBytes: Long,
    monthProgress: Float?,
    threeDayProgress: Float?,
    signalProgress: Float?,
    oledSaver: Boolean,
    onNavigate: (AppScreen) -> Unit,
    onPhone: () -> Unit,
    onUsage: () -> Unit,
) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HomeCard(Modifier.weight(1.45f).fillMaxHeight()) {
                Text("Data usage", color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Spacer(Modifier.height(3.dp))
                Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    HomeUsageGauge(
                        "1 month", "Current cycle",
                        if (snapshot.usageAccessGranted) snapshot.monthBytes?.let(::homeFormatBytes) ?: "0 B" else "Enable",
                        if (snapshot.usageAccessGranted) "/ ${homeFormatBytes(monthlyLimitBytes)}" else "Tap to allow",
                        monthProgress, HomeCyan, 88.dp,
                        if (snapshot.usageAccessGranted) null else onUsage,
                    )
                    HomeUsageGauge(
                        "3 days", "Recent usage",
                        if (snapshot.usageAccessGranted) snapshot.threeDayBytes?.let(::homeFormatBytes) ?: "0 B" else "Enable",
                        if (snapshot.usageAccessGranted) "Mobile data" else "Tap to allow",
                        threeDayProgress, HomeGreen, 88.dp,
                        if (snapshot.usageAccessGranted) null else onUsage,
                    )
                }
            }
            HomeCard(Modifier.weight(.75f).fillMaxHeight()) {
                HomeNetworkGauge(snapshot, signalProgress, if (snapshot.phonePermissionGranted) null else onPhone, 106.dp)
            }
            Column(Modifier.weight(.92f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                HomeAction(Modifier.weight(1f), Icons.Rounded.Wifi, "Wi-Fi", homeHotspotLabel(hotspotActive), HomeBlue) { onNavigate(AppScreen.HOTSPOT) }
                HomeAction(Modifier.weight(1f), Icons.Rounded.SwapVert, "Communication mode", hotspotBand.ifBlank { "System" }, HomeSlate) { onNavigate(AppScreen.DATA) }
                HomeAction(Modifier.weight(1f), Icons.Rounded.Brightness6, "Display", if (oledSaver) "OLED Saver on" else "Screen", HomeTeal) { onNavigate(AppScreen.DISPLAY) }
                HomeAction(Modifier.weight(1f), Icons.Rounded.Settings, "System settings", "ZeroFi settings", HomePurple) { onNavigate(AppScreen.SETTINGS) }
            }
        }

        HomeCard(Modifier.fillMaxWidth().height(58.dp).clickable { onNavigate(AppScreen.DEVICES) }) {
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                HomeCircleIcon(Icons.Rounded.Devices, HomeBlue)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Connected devices", color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(if (hotspotActive == true) "Hotspot active · tap to view clients" else homeHotspotLabel(hotspotActive), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("↓ ${snapshot.downloadBytesPerSecond?.let(::homeFormatRate) ?: "—"}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp)
                    Text("↑ ${snapshot.uploadBytesPerSecond?.let(::homeFormatRate) ?: "—"}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp)
                }
            }
        }
    }
}

@Composable
private fun HomeActionGrid(
    modifier: Modifier,
    hotspotActive: Boolean?,
    hotspotBand: String,
    oledSaver: Boolean,
    onNavigate: (AppScreen) -> Unit,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            HomeAction(Modifier.weight(1f).fillMaxHeight(), Icons.Rounded.Wifi, "Wi-Fi", homeHotspotLabel(hotspotActive), HomeBlue) { onNavigate(AppScreen.HOTSPOT) }
            HomeAction(Modifier.weight(1f).fillMaxHeight(), Icons.Rounded.SwapVert, "Communication mode", hotspotBand.ifBlank { "System" }, HomeSlate) { onNavigate(AppScreen.DATA) }
        }
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            HomeAction(Modifier.weight(1f).fillMaxHeight(), Icons.Rounded.Brightness6, "Display", if (oledSaver) "OLED Saver on" else "Screen", HomeTeal) { onNavigate(AppScreen.DISPLAY) }
            HomeAction(Modifier.weight(1f).fillMaxHeight(), Icons.Rounded.Settings, "System settings", "ZeroFi settings", HomePurple) { onNavigate(AppScreen.SETTINGS) }
        }
    }
}

@Composable
private fun HomeUsageGauge(
    label: String,
    period: String,
    value: String,
    detail: String,
    progress: Float?,
    accent: Color,
    size: Dp = 98.dp,
    onClick: (() -> Unit)? = null,
) {
    Column(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = MaterialTheme.colorScheme.onSurface, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        Text(period, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 6.5.sp)
        Spacer(Modifier.height(2.dp))
        Box(Modifier.size(size), contentAlignment = Alignment.Center) {
            HomeRing(progress, accent, size)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(value, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 6.5.sp, textAlign = TextAlign.Center, maxLines = 2)
            }
        }
    }
}

@Composable
private fun HomeNetworkGauge(snapshot: SystemSnapshot, progress: Float?, onClick: (() -> Unit)?, size: Dp = 110.dp) {
    Column(
        Modifier.fillMaxSize().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Network status", color = MaterialTheme.colorScheme.onSurface, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(3.dp))
        Box(Modifier.size(size), contentAlignment = Alignment.Center) {
            HomeRing(progress, HomeBlue, size, 130f, 280f)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (!snapshot.phonePermissionGranted) "Allow" else snapshot.networkType.ifBlank { "Network" }, color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                Text(if (!snapshot.phonePermissionGranted) "Phone status" else snapshot.signalDbm?.let { "$it dBm" } ?: snapshot.carrierName.ifBlank { "Signal" }, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 7.sp, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun HomeDevices(snapshot: SystemSnapshot, hotspotActive: Boolean?) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("Connected devices", color = MaterialTheme.colorScheme.onSurface, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Icon(Icons.Rounded.Devices, null, tint = HomeBlue, modifier = Modifier.size(26.dp))
        Spacer(Modifier.height(3.dp))
        Text(if (hotspotActive == true) "—" else "0", color = MaterialTheme.colorScheme.onSurface, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
        Text(if (hotspotActive == true) "Client count OEM restricted" else homeHotspotLabel(hotspotActive), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 7.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text("↓ ${snapshot.downloadBytesPerSecond?.let(::homeFormatRate) ?: "—"}   ↑ ${snapshot.uploadBytesPerSecond?.let(::homeFormatRate) ?: "—"}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 6.5.sp)
    }
}

@Composable
private fun HomeAction(modifier: Modifier, icon: ImageVector, title: String, subtitle: String, accent: Color, onClick: () -> Unit) {
    val shape = RoundedCornerShape(15.dp)
    Row(
        modifier.background(MaterialTheme.colorScheme.surface, shape)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .14f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HomeCircleIcon(icon, accent)
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 7.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun HomeCard(modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(15.dp)
    Column(
        modifier.background(MaterialTheme.colorScheme.surface, shape)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .13f), shape)
            .padding(10.dp),
        content = content,
    )
}

@Composable
private fun HomeRing(progress: Float?, accent: Color, size: Dp, start: Float = -90f, maxSweep: Float = 360f) {
    val animated by animateFloatAsState(progress?.coerceIn(0f, 1f) ?: 0f, label = "homeGauge")
    val track = MaterialTheme.colorScheme.outline.copy(alpha = .16f)
    Canvas(Modifier.size(size)) {
        val stroke = Stroke(width = size.toPx() * .055f, cap = StrokeCap.Round)
        drawArc(track, start, maxSweep, false, style = stroke)
        drawArc(if (progress == null) accent.copy(alpha = .35f) else accent, start, if (progress == null) maxSweep * .10f else maxSweep * animated, false, style = stroke)
    }
}

@Composable private fun HomeCircleIcon(icon: ImageVector, accent: Color) {
    Box(Modifier.size(29.dp).background(accent.copy(alpha = .16f), CircleShape), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = accent, modifier = Modifier.size(16.dp))
    }
}

@Composable private fun HomeHeaderIcon(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.size(30.dp).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .58f), CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, label, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp))
    }
}

private fun homeHotspotLabel(active: Boolean?): String = when (active) {
    true -> "On"
    false -> "Off"
    null -> "System managed"
}

private fun homeFormatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format(Locale.US, "%.0f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format(Locale.US, "%.1f MB", mb)
    return String.format(Locale.US, "%.2f GB", mb / 1024.0)
}

private fun homeFormatRate(bytes: Long): String {
    if (bytes < 1024) return "$bytes B/s"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format(Locale.US, "%.0f KB/s", kb)
    return String.format(Locale.US, "%.1f MB/s", kb / 1024.0)
}

private const val HOME_GIB = 1024L * 1024L * 1024L