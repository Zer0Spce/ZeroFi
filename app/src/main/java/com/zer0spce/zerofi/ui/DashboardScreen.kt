package com.zer0spce.zerofi.ui

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.Brightness6
import androidx.compose.material.icons.rounded.DataUsage
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.SwapVert
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
import androidx.compose.ui.text.style.TextAlign
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
    val background = when {
        oledSaver -> Brush.linearGradient(listOf(Color.Black, Color.Black))
        darkMode -> Brush.radialGradient(
            colors = listOf(accent.copy(alpha = 0.08f), MaterialTheme.colorScheme.background),
            radius = 1500f,
        )
        else -> Brush.linearGradient(
            listOf(
                MaterialTheme.colorScheme.background,
                accent.copy(alpha = 0.035f),
                MaterialTheme.colorScheme.background,
            ),
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
        modifier = Modifier
            .fillMaxSize()
            .background(background),
    ) {
        val compact = maxHeight < 430.dp || maxWidth < 800.dp
        val outerPadding = if (compact) 10.dp else 18.dp
        val gap = if (compact) 8.dp else 14.dp
        val monthlyLimitBytes = (dataLimitGb * GIB).toLong().coerceAtLeast(1L)
        val monthProgress = snapshot.monthBytes?.let { (it.toFloat() / monthlyLimitBytes).coerceIn(0f, 1f) }
        val threeDayTarget = (monthlyLimitBytes * 3L / 30L).coerceAtLeast(1L)
        val threeDayProgress = snapshot.threeDayBytes?.let { (it.toFloat() / threeDayTarget).coerceIn(0f, 1f) }
        val signalProgress = snapshot.signalLevel?.let { (it.coerceIn(0, 4) / 4f).coerceIn(0f, 1f) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(outerPadding)
                .offset(x = driftX, y = driftY),
        ) {
            ScrHeader(
                snapshot = snapshot,
                accentTheme = accentTheme,
                darkMode = darkMode,
                compact = compact,
                onAccentThemeChange = onAccentThemeChange,
                onDarkModeChange = onDarkModeChange,
            )

            Spacer(Modifier.height(gap))

            ScrMainPanel(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                snapshot = snapshot,
                hotspotActive = hotspotActive,
                hotspotBand = hotspotBand,
                monthlyLimitBytes = monthlyLimitBytes,
                monthProgress = monthProgress,
                threeDayProgress = threeDayProgress,
                signalProgress = signalProgress,
                compact = compact,
                oledSaver = oledSaver,
                onRequestPhonePermission = onRequestPhonePermission,
                onRequestUsageAccess = onRequestUsageAccess,
                onNavigate = onNavigate,
            )
        }
    }
}

@Composable
private fun ScrHeader(
    snapshot: SystemSnapshot,
    accentTheme: AccentTheme,
    darkMode: Boolean,
    compact: Boolean,
    onAccentThemeChange: (AccentTheme) -> Unit,
    onDarkModeChange: (Boolean) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = "ZeroFi",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = if (compact) 24.sp else 31.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                text = "5G MOBILE WI-FI",
                color = MaterialTheme.colorScheme.primary,
                fontSize = if (compact) 8.sp else 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.6.sp,
            )
        }

        Spacer(Modifier.weight(1f))

        HeaderChip(
            icon = Icons.Rounded.SignalCellularAlt,
            text = if (snapshot.validatedInternet) "Online" else "Offline",
            compact = compact,
        )
        Spacer(Modifier.width(6.dp))
        HeaderChip(
            icon = Icons.Rounded.BatteryFull,
            text = buildString {
                append(snapshot.batteryPercent?.let { "$it%" } ?: "—")
                if (snapshot.batteryTimeRemainingText.isNotBlank()) {
                    append(" · ")
                    append(snapshot.batteryTimeRemainingText)
                }
            },
            compact = compact,
        )
        Spacer(Modifier.width(8.dp))

        Box {
            HeaderControl(
                text = accentTheme.label,
                trailing = Icons.Rounded.ExpandMore,
                compact = compact,
                onClick = { menuOpen = true },
            )
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
            ) {
                AccentTheme.entries.forEach { theme ->
                    DropdownMenuItem(
                        text = { Text(theme.label) },
                        leadingIcon = {
                            Box(Modifier.size(12.dp).background(theme.accent, CircleShape))
                        },
                        onClick = {
                            onAccentThemeChange(theme)
                            menuOpen = false
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
private fun ScrMainPanel(
    modifier: Modifier,
    snapshot: SystemSnapshot,
    hotspotActive: Boolean?,
    hotspotBand: String,
    monthlyLimitBytes: Long,
    monthProgress: Float?,
    threeDayProgress: Float?,
    signalProgress: Float?,
    compact: Boolean,
    oledSaver: Boolean,
    onRequestPhonePermission: () -> Unit,
    onRequestUsageAccess: () -> Unit,
    onNavigate: (AppScreen) -> Unit,
) {
    val shape = RoundedCornerShape(if (compact) 22.dp else 30.dp)
    val accent = MaterialTheme.colorScheme.primary

    Column(
        modifier = modifier
            .shadow(
                elevation = if (oledSaver) 0.dp else 9.dp,
                shape = shape,
                ambientColor = accent.copy(alpha = 0.07f),
                spotColor = accent.copy(alpha = 0.10f),
            )
            .background(MaterialTheme.colorScheme.surface, shape)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f), shape)
            .padding(horizontal = if (compact) 12.dp else 22.dp, vertical = if (compact) 10.dp else 18.dp),
    ) {
        Row(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ScrCircleStatus(
                modifier = Modifier.weight(1f),
                title = "This month",
                main = snapshot.monthBytes?.let(::formatBytes) ?: "—",
                secondary = if (snapshot.usageAccessGranted) {
                    "of ${formatBytes(monthlyLimitBytes)}"
                } else {
                    "Usage Access needed"
                },
                progress = monthProgress,
                compact = compact,
                onClick = if (snapshot.usageAccessGranted) null else onRequestUsageAccess,
            )

            ScrCircleStatus(
                modifier = Modifier.weight(1f),
                title = "Last 3 days",
                main = snapshot.threeDayBytes?.let(::formatBytes) ?: "—",
                secondary = if (snapshot.usageAccessGranted) "Mobile data" else "Usage Access needed",
                progress = threeDayProgress,
                compact = compact,
                onClick = if (snapshot.usageAccessGranted) null else onRequestUsageAccess,
            )

            ScrCircleStatus(
                modifier = Modifier.weight(1f),
                title = "Mobile network",
                main = snapshot.networkType,
                secondary = when {
                    !snapshot.phonePermissionGranted -> "Phone permission needed"
                    snapshot.signalDbm != null -> "${snapshot.signalDbm} dBm"
                    snapshot.signalLevel != null -> "Signal ${snapshot.signalLevel}/4"
                    else -> snapshot.carrierName
                },
                progress = signalProgress,
                compact = compact,
                centerIcon = Icons.Rounded.SignalCellularAlt,
                onClick = if (snapshot.phonePermissionGranted) null else onRequestPhonePermission,
            )
        }

        ScrDivider()

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(if (compact) 7.dp else 12.dp),
        ) {
            ScrQuickAction(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.Wifi,
                title = "Wi-Fi",
                subtitle = when (hotspotActive) {
                    true -> "Hotspot ON"
                    false -> "Hotspot OFF"
                    null -> "Hotspot"
                },
                compact = compact,
                onClick = { onNavigate(AppScreen.HOTSPOT) },
            )
            ScrQuickAction(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.SwapVert,
                title = "Communication",
                subtitle = hotspotBand.ifBlank { "System" },
                compact = compact,
                onClick = { onNavigate(AppScreen.DATA) },
            )
            ScrQuickAction(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.PowerSettingsNew,
                title = "Power saving",
                subtitle = "Battery controls",
                compact = compact,
                onClick = { onNavigate(AppScreen.SETTINGS) },
            )
            ScrQuickAction(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.Brightness6,
                title = "Display",
                subtitle = "Screen controls",
                compact = compact,
                onClick = { onNavigate(AppScreen.DISPLAY) },
            )
            ScrQuickAction(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.Settings,
                title = "Settings",
                subtitle = "Router mode",
                compact = compact,
                onClick = { onNavigate(AppScreen.SETTINGS) },
            )
        }

        ScrDivider()

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigate(AppScreen.DEVICES) }
                    .padding(vertical = if (compact) 5.dp else 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(if (compact) 34.dp else 42.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Devices,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(if (compact) 18.dp else 22.dp),
                    )
                }
                Column {
                    Text(
                        text = "Connected devices",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = if (compact) 10.sp else 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Tap to view hotspot clients",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = if (compact) 8.sp else 10.sp,
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ThroughputBadge(Icons.Rounded.DataUsage, snapshot.downloadBytesPerSecond, "↓", compact)
                ThroughputBadge(Icons.Rounded.DataUsage, snapshot.uploadBytesPerSecond, "↑", compact)
                Text(
                    text = "1 / 1",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = if (compact) 9.sp else 10.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun ScrCircleStatus(
    modifier: Modifier,
    title: String,
    main: String,
    secondary: String,
    progress: Float?,
    compact: Boolean,
    centerIcon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
) {
    val size = if (compact) 116.dp else 154.dp
    val clickModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier

    Column(
        modifier = modifier.then(clickModifier),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = if (compact) 9.sp else 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(if (compact) 4.dp else 8.dp))
        Box(
            modifier = Modifier.size(size),
            contentAlignment = Alignment.Center,
        ) {
            CircularMeter(progress = progress, size = size)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                centerIcon?.let {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(if (compact) 20.dp else 25.dp),
                    )
                    Spacer(Modifier.height(2.dp))
                }
                Text(
                    text = main,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = if (compact) 16.sp else 21.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = secondary,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = if (compact) 7.sp else 9.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun CircularMeter(progress: Float?, size: Dp) {
    val target = progress?.coerceIn(0f, 1f) ?: 0f
    val animated by animateFloatAsState(targetValue = target, label = "scrCircleMeter")
    val accent = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)

    Canvas(modifier = Modifier.size(size)) {
        val stroke = Stroke(width = size.toPx() * 0.065f, cap = StrokeCap.Round)
        drawArc(
            color = track,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            style = stroke,
        )
        drawArc(
            color = if (progress == null) accent.copy(alpha = 0.28f) else accent,
            startAngle = -90f,
            sweepAngle = if (progress == null) 42f else 360f * animated,
            useCenter = false,
            style = stroke,
        )
    }
}

@Composable
private fun ScrQuickAction(
    modifier: Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String,
    compact: Boolean,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.965f else 1f, label = "scrActionPress")
    val shape = RoundedCornerShape(if (compact) 14.dp else 18.dp)

    Column(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f), shape)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.30f), shape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = if (compact) 8.dp else 12.dp, vertical = if (compact) 8.dp else 11.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(if (compact) 31.dp else 38.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(if (compact) 17.dp else 21.dp),
            )
        }
        Spacer(Modifier.height(if (compact) 4.dp else 7.dp))
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = if (compact) 8.sp else 10.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = subtitle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = if (compact) 7.sp else 8.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ScrDivider() {
    Spacer(Modifier.height(8.dp))
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
    )
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun HeaderChip(icon: ImageVector, text: String, compact: Boolean) {
    Row(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.32f), RoundedCornerShape(14.dp))
            .padding(horizontal = if (compact) 8.dp else 10.dp, vertical = if (compact) 6.dp else 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = if (compact) 8.sp else 9.sp,
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
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, label = "scrHeaderPress")
    val shape = RoundedCornerShape(14.dp)

    Row(
        modifier = Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .background(MaterialTheme.colorScheme.surface, shape)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.34f), shape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = if (compact) 8.dp else 10.dp, vertical = if (compact) 6.dp else 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = if (compact) 8.sp else 9.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
        Icon(
            imageVector = trailing,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(14.dp),
        )
    }
}

@Composable
private fun ThroughputBadge(icon: ImageVector, bytesPerSecond: Long?, prefix: String, compact: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(13.dp),
        )
        Text(
            text = "$prefix ${bytesPerSecond?.let(::formatRate) ?: "—"}",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = if (compact) 7.sp else 9.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format(Locale.US, "%.0f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format(Locale.US, "%.1f MB", mb)
    val gb = mb / 1024.0
    return String.format(Locale.US, "%.2f GB", gb)
}

private fun formatRate(bytesPerSecond: Long): String {
    if (bytesPerSecond < 1024L) return "$bytesPerSecond B/s"
    val kb = bytesPerSecond / 1024.0
    if (kb < 1024.0) return String.format(Locale.US, "%.0f KB/s", kb)
    val mb = kb / 1024.0
    return String.format(Locale.US, "%.1f MB/s", mb)
}

private const val GIB = 1024L * 1024L * 1024L
