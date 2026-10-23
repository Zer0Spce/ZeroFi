package com.zer0spce.zerofi.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.Brightness6
import androidx.compose.material.icons.rounded.DataUsage
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zer0spce.zerofi.platform.SystemSnapshot
import com.zer0spce.zerofi.ui.theme.AccentTheme
import java.util.Locale
import kotlinx.coroutines.delay

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
) {
    val accent = MaterialTheme.colorScheme.primary
    val backgroundBrush = if (oledSaver) {
        Brush.linearGradient(listOf(Color.Black, Color.Black))
    } else {
        Brush.radialGradient(
            colors = listOf(
                accent.copy(alpha = if (darkMode) 0.12f else 0.08f),
                MaterialTheme.colorScheme.background,
            ),
            radius = 1300f,
        )
    }

    var driftStep by remember { mutableIntStateOf(0) }
    LaunchedEffect(oledSaver) {
        driftStep = 0
        if (oledSaver) {
            while (true) {
                delay(60_000L)
                driftStep = (driftStep + 1) % 8
            }
        }
    }
    val driftX = if (oledSaver) listOf((-2).dp, 0.dp, 2.dp, 1.dp, (-1).dp, 2.dp, 0.dp, (-2).dp)[driftStep] else 0.dp
    val driftY = if (oledSaver) listOf(0.dp, 2.dp, 1.dp, (-2).dp, 2.dp, 0.dp, (-1).dp, (-2).dp)[driftStep] else 0.dp

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().background(backgroundBrush),
    ) {
        val compact = maxHeight < 430.dp || maxWidth < 800.dp
        val outerPadding = if (compact) 10.dp else 18.dp
        val gap = if (compact) 8.dp else 13.dp
        val monthlyLimitBytes = (dataLimitGb * GIB).toLong().coerceAtLeast(1L)
        val monthProgress = snapshot.monthBytes?.let { it.toFloat() / monthlyLimitBytes.toFloat() }
        val signalProgress = snapshot.signalLevel?.let { it.coerceIn(0, 4) / 4f }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(outerPadding)
                .offset(x = driftX, y = driftY),
        ) {
            DashboardHeader(
                snapshot = snapshot,
                accentTheme = accentTheme,
                darkMode = darkMode,
                compact = compact,
                onAccentThemeChange = onAccentThemeChange,
                onDarkModeChange = onDarkModeChange,
            )

            Spacer(Modifier.height(gap))

            Row(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(gap),
            ) {
                GaugeCard(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    title = "This month",
                    value = snapshot.monthBytes?.let(::formatBytes) ?: "—",
                    detail = if (snapshot.usageAccessGranted) {
                        "${formatBytes(monthlyLimitBytes)} limit"
                    } else {
                        "Tap for Usage Access"
                    },
                    progress = monthProgress,
                    compact = compact,
                    oledSaver = oledSaver,
                    onClick = if (snapshot.usageAccessGranted) null else onRequestUsageAccess,
                )

                GaugeCard(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    title = "Last 3 days",
                    value = snapshot.threeDayBytes?.let(::formatBytes) ?: "—",
                    detail = if (snapshot.usageAccessGranted) "Mobile data" else "Usage Access needed",
                    progress = null,
                    compact = compact,
                    oledSaver = oledSaver,
                    onClick = if (snapshot.usageAccessGranted) null else onRequestUsageAccess,
                )

                GaugeCard(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    title = snapshot.carrierName,
                    value = snapshot.networkType,
                    detail = when {
                        !snapshot.phonePermissionGranted -> "Tap for phone permission"
                        snapshot.signalDbm != null -> "${snapshot.signalDbm} dBm"
                        snapshot.signalLevel != null -> "Signal ${snapshot.signalLevel}/4"
                        else -> "Waiting for signal"
                    },
                    progress = signalProgress,
                    compact = compact,
                    oledSaver = oledSaver,
                    onClick = if (snapshot.phonePermissionGranted) null else onRequestPhonePermission,
                )

                StatusPanel(
                    modifier = Modifier.widthIn(min = if (compact) 160.dp else 218.dp).fillMaxHeight(),
                    snapshot = snapshot,
                    hotspotActive = hotspotActive,
                    hotspotBand = hotspotBand,
                    compact = compact,
                )
            }

            Spacer(Modifier.height(gap))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(gap),
            ) {
                ActionTile(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.Wifi,
                    title = "Hotspot",
                    subtitle = when (hotspotActive) {
                        true -> "ON"
                        false -> "OFF"
                        null -> "System controls"
                    },
                    compact = compact,
                    oledSaver = oledSaver,
                    onClick = { onNavigate(AppScreen.HOTSPOT) },
                )
                ActionTile(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.Devices,
                    title = "Devices",
                    subtitle = "Clients",
                    compact = compact,
                    oledSaver = oledSaver,
                    onClick = { onNavigate(AppScreen.DEVICES) },
                )
                ActionTile(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.DataUsage,
                    title = "Data",
                    subtitle = "Usage & limits",
                    compact = compact,
                    oledSaver = oledSaver,
                    onClick = { onNavigate(AppScreen.DATA) },
                )
                ActionTile(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.Brightness6,
                    title = "Display",
                    subtitle = if (oledSaver) "OLED Saver ON" else "Screen controls",
                    compact = compact,
                    oledSaver = oledSaver,
                    onClick = { onNavigate(AppScreen.DISPLAY) },
                )
                ActionTile(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.Settings,
                    title = "Settings",
                    subtitle = "Router mode",
                    compact = compact,
                    oledSaver = oledSaver,
                    onClick = { onNavigate(AppScreen.SETTINGS) },
                )
            }
        }
    }
}

@Composable
private fun DashboardHeader(
    snapshot: SystemSnapshot,
    accentTheme: AccentTheme,
    darkMode: Boolean,
    compact: Boolean,
    onAccentThemeChange: (AccentTheme) -> Unit,
    onDarkModeChange: (Boolean) -> Unit,
) {
    var themeMenuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = "ZeroFi",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = if (compact) 25.sp else 32.sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = "POCKET WI-FI ROUTER",
                color = MaterialTheme.colorScheme.primary,
                fontSize = if (compact) 8.sp else 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.7.sp,
            )
        }

        Spacer(Modifier.weight(1f))

        HeaderStatus(
            icon = Icons.Rounded.SignalCellularAlt,
            text = if (snapshot.validatedInternet) "Online" else "No internet",
            compact = compact,
        )
        Spacer(Modifier.width(6.dp))
        HeaderStatus(
            icon = Icons.Rounded.BatteryFull,
            text = buildString {
                append(snapshot.batteryPercent?.let { "$it%" } ?: "—")
                append(" · ")
                append(snapshot.batteryTimeRemainingText)
            },
            compact = compact,
        )
        Spacer(Modifier.width(8.dp))

        Box {
            HeaderControl(
                text = accentTheme.label,
                trailing = Icons.Rounded.ExpandMore,
                compact = compact,
                onClick = { themeMenuOpen = true },
            )
            DropdownMenu(
                expanded = themeMenuOpen,
                onDismissRequest = { themeMenuOpen = false },
            ) {
                AccentTheme.entries.forEach { theme ->
                    DropdownMenuItem(
                        text = { Text(theme.label) },
                        leadingIcon = {
                            Box(Modifier.size(12.dp).background(theme.accent, CircleShape))
                        },
                        onClick = {
                            onAccentThemeChange(theme)
                            themeMenuOpen = false
                        },
                    )
                }
            }
        }

        Spacer(Modifier.width(6.dp))
        HeaderControl(
            text = if (darkMode) "Dark" else "Light",
            trailing = Icons.Rounded.LightMode,
            compact = compact,
            onClick = { onDarkModeChange(!darkMode) },
        )
    }
}

@Composable
private fun HeaderStatus(
    icon: ImageVector,
    text: String,
    compact: Boolean,
) {
    Row(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.88f), RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
            .padding(horizontal = if (compact) 8.dp else 11.dp, vertical = if (compact) 6.dp else 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp))
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = if (compact) 8.sp else 10.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
private fun HeaderControl(
    text: String,
    trailing: ImageVector,
    compact: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, label = "headerControlScale")

    Row(
        modifier = Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.86f), shape)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.75f), shape)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = if (compact) 9.dp else 13.dp, vertical = if (compact) 7.dp else 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = if (compact) 9.sp else 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
        Icon(trailing, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun GaugeCard(
    modifier: Modifier,
    title: String,
    value: String,
    detail: String,
    progress: Float?,
    compact: Boolean,
    oledSaver: Boolean,
    onClick: (() -> Unit)? = null,
) {
    val accent = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(if (compact) 15.dp else 20.dp)
    val clickableModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier

    Column(
        modifier = modifier
            .shadow(
                if (oledSaver) 1.dp else 10.dp,
                shape,
                ambientColor = accent.copy(alpha = if (oledSaver) 0.02f else 0.10f),
                spotColor = accent.copy(alpha = if (oledSaver) 0.03f else 0.16f),
            )
            .background(MaterialTheme.colorScheme.surface.copy(alpha = if (oledSaver) 1f else 0.95f), shape)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.60f), shape)
            .then(clickableModifier)
            .padding(if (compact) 10.dp else 15.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = if (compact) 9.sp else 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Gauge(value = value, progress = progress, size = if (compact) 72.dp else 104.dp)
        Text(
            text = detail,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
            fontSize = if (compact) 8.sp else 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun Gauge(value: String, progress: Float?, size: Dp) {
    val accent = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.outline.copy(alpha = 0.38f)
    val animatedProgress by animateFloatAsState(progress?.coerceIn(0f, 1f) ?: 0f, label = "gaugeProgress")

    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val strokeWidth = size.toPx() * 0.085f
            drawArc(
                color = track,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
            drawArc(
                brush = Brush.sweepGradient(listOf(accent.copy(alpha = 0.52f), accent)),
                startAngle = 135f,
                sweepAngle = 270f * animatedProgress,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
        }
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = if (size < 90.dp) 14.sp else 19.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

@Composable
private fun StatusPanel(
    modifier: Modifier,
    snapshot: SystemSnapshot,
    hotspotActive: Boolean?,
    hotspotBand: String,
    compact: Boolean,
) {
    val shape = RoundedCornerShape(if (compact) 15.dp else 20.dp)
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f), shape)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.60f), shape)
            .padding(if (compact) 10.dp else 14.dp),
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        StatusRow("Internet", if (snapshot.validatedInternet) "Online" else "Offline", compact)
        StatusRow("Hotspot", when (hotspotActive) { true -> "On"; false -> "Off"; null -> "System" }, compact)
        StatusRow("Wi-Fi band", hotspotBand, compact)
        StatusRow("Battery", snapshot.batteryPercent?.let { "$it%${if (snapshot.charging) " · charging" else ""}" } ?: "—", compact)
        StatusRow("Battery time", snapshot.batteryTimeRemainingText, compact)
        StatusRow("Temperature", snapshot.batteryTempC?.let { String.format(Locale.US, "%.1f°C", it) } ?: "—", compact)
        StatusRow("Uptime", snapshot.uptimeText, compact)
    }
}

@Composable
private fun StatusRow(label: String, value: String, compact: Boolean) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(if (compact) 6.dp else 7.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
        Spacer(Modifier.width(7.dp))
        Column(Modifier.weight(1f)) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = if (compact) 7.sp else 9.sp)
            Text(
                value,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = if (compact) 9.sp else 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ActionTile(
    modifier: Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String,
    compact: Boolean,
    oledSaver: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val accent = MaterialTheme.colorScheme.primary
    val scale by animateFloatAsState(if (pressed) 0.965f else 1f, label = "actionTileScale")
    val background by animateColorAsState(
        if (pressed) accent.copy(alpha = if (oledSaver) 0.10f else 0.17f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (oledSaver) 1f else 0.76f),
        label = "actionTileColor",
    )
    val shape = RoundedCornerShape(if (compact) 13.dp else 17.dp)

    Row(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(
                if (oledSaver) 1.dp else if (pressed) 3.dp else 8.dp,
                shape,
                ambientColor = accent.copy(alpha = if (oledSaver) 0.02f else if (pressed) 0.06f else 0.11f),
                spotColor = accent.copy(alpha = if (oledSaver) 0.03f else if (pressed) 0.08f else 0.16f),
            )
            .background(background, shape)
            .border(1.dp, if (pressed) accent.copy(alpha = 0.65f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.55f), shape)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = if (compact) 9.dp else 13.dp, vertical = if (compact) 8.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(if (compact) 18.dp else 23.dp))
        Spacer(Modifier.width(if (compact) 6.dp else 9.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = if (compact) 9.sp else 12.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = if (compact) 7.sp else 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

internal const val GIB = 1024f * 1024f * 1024f

internal fun formatBytes(bytes: Long): String {
    val value = bytes.coerceAtLeast(0L).toDouble()
    return when {
        value >= GIB -> String.format(Locale.US, "%.2f GB", value / GIB)
        value >= 1024.0 * 1024.0 -> String.format(Locale.US, "%.1f MB", value / (1024.0 * 1024.0))
        value >= 1024.0 -> String.format(Locale.US, "%.1f KB", value / 1024.0)
        else -> "${value.toLong()} B"
    }
}
