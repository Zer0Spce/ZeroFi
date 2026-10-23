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

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val portrait = maxHeight > maxWidth
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = if (portrait) 12.dp else 16.dp, vertical = 8.dp)
        ) {
            Scr01Header(
                lockRunning = lockRunning,
                onMenu = { onNavigate(AppScreen.SETTINGS) },
                onInfo = { onNavigate(AppScreen.DIAGNOSTICS) },
                onLock = onRouterLockToggle,
                onQr = { onNavigate(AppScreen.HOTSPOT) },
            )
            Spacer(Modifier.height(8.dp))
            if (portrait) {
                Scr01Portrait(
                    snapshot = snapshot,
                    hotspotActive = hotspotActive,
                    hotspotBand = hotspotBand,
                    monthlyLimitBytes = monthlyLimitBytes,
                    monthProgress = monthProgress,
                    threeDayProgress = threeDayProgress,
                    signalProgress = signalProgress,
                    onNavigate = onNavigate,
                    onPhone = onRequestPhonePermission,
                    onUsage = onRequestUsageAccess,
                )
            } else {
                Scr01Landscape(
                    snapshot = snapshot,
                    hotspotActive = hotspotActive,
                    hotspotBand = hotspotBand,
                    monthlyLimitBytes = monthlyLimitBytes,
                    monthProgress = monthProgress,
                    threeDayProgress = threeDayProgress,
                    signalProgress = signalProgress,
                    onNavigate = onNavigate,
                    onPhone = onRequestPhonePermission,
                    onUsage = onRequestUsageAccess,
                )
            }
        }
    }
}

@Composable
private fun Scr01Header(
    lockRunning: Boolean,
    onMenu: () -> Unit,
    onInfo: () -> Unit,
    onLock: () -> Unit,
    onQr: () -> Unit,
) {
    Box(Modifier.fillMaxWidth().height(38.dp), contentAlignment = Alignment.Center) {
        HeaderIcon(Icons.Rounded.Menu, "Menu", onMenu, Modifier.align(Alignment.CenterStart))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "ZeroFi",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                "Mobile Wi-Fi",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 7.sp,
            )
        }
        Row(
            Modifier.align(Alignment.CenterEnd),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HeaderIcon(Icons.Rounded.Info, "Router info", onInfo)
            HeaderIcon(
                if (lockRunning) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                if (lockRunning) "Unlock ZeroFi" else "Lock ZeroFi",
                onLock,
            )
            HeaderIcon(Icons.Rounded.QrCode, "Wi-Fi QR", onQr)
        }
    }
}

@Composable
private fun Scr01Portrait(
    snapshot: SystemSnapshot,
    hotspotActive: Boolean?,
    hotspotBand: String,
    monthlyLimitBytes: Long,
    monthProgress: Float?,
    threeDayProgress: Float?,
    signalProgress: Float?,
    onNavigate: (AppScreen) -> Unit,
    onPhone: () -> Unit,
    onUsage: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ScrCard(Modifier.fillMaxWidth()) {
            Text(
                "Data usage",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                UsageGauge(
                    label = "1 month",
                    period = "Current cycle",
                    value = if (snapshot.usageAccessGranted) snapshot.monthBytes?.let(::dashboardFormatBytes) ?: "0 B" else "Enable",
                    detail = if (snapshot.usageAccessGranted) "/ ${dashboardFormatBytes(monthlyLimitBytes)}" else "Tap to allow",
                    progress = monthProgress,
                    accent = MaterialTheme.colorScheme.primary,
                    onClick = if (snapshot.usageAccessGranted) null else onUsage,
                )
                UsageGauge(
                    label = "3 days",
                    period = "Recent usage",
                    value = if (snapshot.usageAccessGranted) snapshot.threeDayBytes?.let(::dashboardFormatBytes) ?: "0 B" else "Enable",
                    detail = if (snapshot.usageAccessGranted) "Mobile data" else "Tap to allow",
                    progress = threeDayProgress,
                    accent = MaterialTheme.colorScheme.secondary,
                    onClick = if (snapshot.usageAccessGranted) null else onUsage,
                )
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ScrCard(Modifier.weight(1f).height(154.dp)) {
                NetworkGauge(
                    snapshot = snapshot,
                    progress = signalProgress,
                    onClick = if (snapshot.phonePermissionGranted) null else onPhone,
                )
            }
            ScrCard(
                Modifier
                    .weight(1f)
                    .height(154.dp)
                    .clickable { onNavigate(AppScreen.DEVICES) }
            ) {
                ConnectedDevicesPanel(snapshot, hotspotActive)
            }
        }

        ActionGrid(hotspotActive, hotspotBand, onNavigate)

        StatusFooter(snapshot, hotspotActive, hotspotBand) {
            onNavigate(AppScreen.DIAGNOSTICS)
        }
    }
}

@Composable
private fun Scr01Landscape(
    snapshot: SystemSnapshot,
    hotspotActive: Boolean?,
    hotspotBand: String,
    monthlyLimitBytes: Long,
    monthProgress: Float?,
    threeDayProgress: Float?,
    signalProgress: Float?,
    onNavigate: (AppScreen) -> Unit,
    onPhone: () -> Unit,
    onUsage: () -> Unit,
) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier.weight(1f).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ScrCard(Modifier.weight(1.45f).fillMaxHeight()) {
                Text(
                    "Data usage",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(7.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    UsageGauge(
                        label = "1 month",
                        period = "Current cycle",
                        value = if (snapshot.usageAccessGranted) snapshot.monthBytes?.let(::dashboardFormatBytes) ?: "0 B" else "Enable",
                        detail = if (snapshot.usageAccessGranted) "/ ${dashboardFormatBytes(monthlyLimitBytes)}" else "Tap to allow",
                        progress = monthProgress,
                        accent = MaterialTheme.colorScheme.primary,
                        size = 88.dp,
                        onClick = if (snapshot.usageAccessGranted) null else onUsage,
                    )
                    UsageGauge(
                        label = "3 days",
                        period = "Recent usage",
                        value = if (snapshot.usageAccessGranted) snapshot.threeDayBytes?.let(::dashboardFormatBytes) ?: "0 B" else "Enable",
                        detail = if (snapshot.usageAccessGranted) "Mobile data" else "Tap to allow",
                        progress = threeDayProgress,
                        accent = MaterialTheme.colorScheme.secondary,
                        size = 88.dp,
                        onClick = if (snapshot.usageAccessGranted) null else onUsage,
                    )
                }
            }

            ScrCard(Modifier.weight(.72f).fillMaxHeight()) {
                NetworkGauge(
                    snapshot = snapshot,
                    progress = signalProgress,
                    onClick = if (snapshot.phonePermissionGranted) null else onPhone,
                    size = 104.dp,
                )
            }

            Column(
                Modifier.weight(.95f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                LandscapeAction(Icons.Rounded.Wifi, "Wi-Fi", hotspotLabel(hotspotActive)) {
                    onNavigate(AppScreen.HOTSPOT)
                }
                LandscapeAction(Icons.Rounded.SwapVert, "Communication mode", hotspotBand.ifBlank { "System" }) {
                    onNavigate(AppScreen.DATA)
                }
                LandscapeAction(Icons.Rounded.PowerSettingsNew, "Power saving", batterySubtitle(snapshot)) {
                    onNavigate(AppScreen.SETTINGS)
                }
                LandscapeAction(Icons.Rounded.Settings, "System settings", "ZeroFi settings") {
                    onNavigate(AppScreen.SETTINGS)
                }
            }
        }

        ScrCard(
            Modifier
                .fillMaxWidth()
                .height(58.dp)
                .clickable { onNavigate(AppScreen.DEVICES) }
        ) {
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                CircleIcon(Icons.Rounded.Devices)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Connected devices", color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(
                        if (hotspotActive == true) "Hotspot active · tap to view clients" else hotspotLabel(hotspotActive),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 8.sp,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("↓ ${snapshot.downloadBytesPerSecond?.let(::dashboardFormatRate) ?: "—"}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp)
                    Text("↑ ${snapshot.uploadBytesPerSecond?.let(::dashboardFormatRate) ?: "—"}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp)
                }
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun UsageGauge(
    label: String,
    period: String,
    value: String,
    detail: String,
    progress: Float?,
    accent: Color,
    size: Dp = 96.dp,
    onClick: (() -> Unit)? = null,
) {
    Column(
        (if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurface, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        Text(period, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 6.5.sp)
        Spacer(Modifier.height(3.dp))
        Box(Modifier.size(size), contentAlignment = Alignment.Center) {
            GaugeRing(progress, accent, size)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(value, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 6.5.sp, textAlign = TextAlign.Center, maxLines = 2)
            }
        }
    }
}

@Composable
private fun NetworkGauge(
    snapshot: SystemSnapshot,
    progress: Float?,
    onClick: (() -> Unit)?,
    size: Dp = 112.dp,
) {
    Column(
        Modifier
            .fillMaxSize()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Network status", color = MaterialTheme.colorScheme.onSurface, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(5.dp))
        Box(Modifier.size(size), contentAlignment = Alignment.Center) {
            GaugeRing(progress, MaterialTheme.colorScheme.primary, size, sweepStart = 130f, sweepMax = 280f)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (!snapshot.phonePermissionGranted) "Allow" else snapshot.networkType.ifBlank { "Network" },
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    if (!snapshot.phonePermissionGranted) "Phone status" else snapshot.signalDbm?.let { "$it dBm" } ?: snapshot.carrierName.ifBlank { "Signal" },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 7.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun ConnectedDevicesPanel(snapshot: SystemSnapshot, hotspotActive: Boolean?) {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Connected devices", color = MaterialTheme.colorScheme.onSurface, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(14.dp))
        Icon(Icons.Rounded.Devices, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
        Spacer(Modifier.height(7.dp))
        Text(
            if (hotspotActive == true) "—" else "0",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
        )
        Text(
            if (hotspotActive == true) "Client count is OEM restricted" else hotspotLabel(hotspotActive),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 7.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(7.dp))
        Text(
            "↓ ${snapshot.downloadBytesPerSecond?.let(::dashboardFormatRate) ?: "—"}   ↑ ${snapshot.uploadBytesPerSecond?.let(::dashboardFormatRate) ?: "—"}",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 6.5.sp,
        )
    }
}

@Composable
private fun ActionGrid(
    hotspotActive: Boolean?,
    hotspotBand: String,
    onNavigate: (AppScreen) -> Unit,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionTile(Modifier.weight(1f), Icons.Rounded.Wifi, "Wi-Fi", hotspotLabel(hotspotActive)) {
                onNavigate(AppScreen.HOTSPOT)
            }
            ActionTile(Modifier.weight(1f), Icons.Rounded.SwapVert, "Communication mode", hotspotBand.ifBlank { "System" }) {
                onNavigate(AppScreen.DATA)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionTile(Modifier.weight(1f), Icons.Rounded.PowerSettingsNew, "Power saving", "Battery") {
                onNavigate(AppScreen.SETTINGS)
            }
            ActionTile(Modifier.weight(1f), Icons.Rounded.Settings, "System settings", "ZeroFi") {
                onNavigate(AppScreen.SETTINGS)
            }
        }
    }
}

@Composable
private fun ActionTile(
    modifier: Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier
            .background(MaterialTheme.colorScheme.surface, shape)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .14f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircleIcon(icon)
        Spacer(Modifier.width(9.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 7.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun LandscapeAction(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .weight(1f)
            .background(MaterialTheme.colorScheme.surface, shape)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .14f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircleIcon(icon, 26.dp, 14.dp)
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 8.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 6.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
    }
}

@Composable
private fun StatusFooter(
    snapshot: SystemSnapshot,
    hotspotActive: Boolean?,
    hotspotBand: String,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .42f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Info, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "${snapshot.carrierName.ifBlank { "Carrier" }} · ${snapshot.networkType.ifBlank { "Network" }} · ${hotspotBand.ifBlank { "System" }}",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 8.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${if (snapshot.validatedInternet) "Internet connected" else "Internet unavailable"} · ${hotspotLabel(hotspotActive)} · ${snapshot.batteryPercent?.let { "$it%" } ?: "Battery —"}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 7.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun ScrCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier
            .background(MaterialTheme.colorScheme.surface, shape)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .12f), shape)
            .padding(10.dp),
        content = content,
    )
}

@Composable
private fun HeaderIcon(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .size(30.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, description, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(17.dp))
    }
}

@Composable
private fun CircleIcon(icon: ImageVector, container: Dp = 30.dp, iconSize: Dp = 16.dp) {
    Box(
        Modifier
            .size(container)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = .12f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(iconSize))
    }
}

@Composable
private fun GaugeRing(
    progress: Float?,
    accent: Color,
    size: Dp,
    sweepStart: Float = -90f,
    sweepMax: Float = 360f,
) {
    val animated by animateFloatAsState(progress?.coerceIn(0f, 1f) ?: 0f, label = "gauge")
    val track = MaterialTheme.colorScheme.outline.copy(alpha = .17f)
    Canvas(Modifier.size(size)) {
        val stroke = Stroke(width = size.toPx() * .055f, cap = StrokeCap.Round)
        drawArc(track, sweepStart, sweepMax, useCenter = false, style = stroke)
        val sweep = if (progress == null) sweepMax * .12f else animated * sweepMax
        drawArc(if (progress == null) accent.copy(alpha = .35f) else accent, sweepStart, sweep, useCenter = false, style = stroke)
    }
}

private fun hotspotLabel(active: Boolean?): String = when (active) {
    true -> "Wi-Fi on"
    false -> "Wi-Fi off"
    null -> "System managed"
}

private fun batterySubtitle(snapshot: SystemSnapshot): String = when {
    snapshot.charging -> "Charging"
    snapshot.batteryPercent != null -> "${snapshot.batteryPercent}% battery"
    else -> "Battery"
}

private fun dashboardFormatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format(Locale.US, "%.0f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format(Locale.US, "%.1f MB", mb)
    return String.format(Locale.US, "%.2f GB", mb / 1024.0)
}

private fun dashboardFormatRate(bytes: Long): String {
    if (bytes < 1024) return "$bytes B/s"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format(Locale.US, "%.0f KB/s", kb)
    return String.format(Locale.US, "%.1f MB/s", kb / 1024.0)
}

private const val GIB = 1024L * 1024L * 1024L
