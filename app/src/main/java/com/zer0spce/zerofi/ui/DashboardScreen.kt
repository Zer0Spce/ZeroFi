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
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.Brightness6
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Devices
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
        darkMode -> Brush.verticalGradient(
            listOf(
                MaterialTheme.colorScheme.background,
                accent.copy(alpha = 0.035f),
                MaterialTheme.colorScheme.background,
            ),
        )
        else -> Brush.verticalGradient(
            listOf(
                MaterialTheme.colorScheme.background,
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
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
        val portrait = maxHeight > maxWidth
        val monthlyLimitBytes = (dataLimitGb * GIB).toLong().coerceAtLeast(1L)
        val monthProgress = snapshot.monthBytes?.let { (it.toFloat() / monthlyLimitBytes).coerceIn(0f, 1f) }
        val threeDayTarget = (monthlyLimitBytes * 3L / 30L).coerceAtLeast(1L)
        val threeDayProgress = snapshot.threeDayBytes?.let { (it.toFloat() / threeDayTarget).coerceIn(0f, 1f) }
        val signalProgress = snapshot.signalLevel?.let { it.coerceIn(0, 4) / 4f }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .offset(x = driftX, y = driftY)
                .padding(horizontal = if (portrait) 14.dp else 18.dp, vertical = 12.dp),
        ) {
            CleanHeader(
                snapshot = snapshot,
                accentTheme = accentTheme,
                darkMode = darkMode,
                portrait = portrait,
                onAccentThemeChange = onAccentThemeChange,
                onDarkModeChange = onDarkModeChange,
                onSettings = { onNavigate(AppScreen.SETTINGS) },
            )

            Spacer(Modifier.height(10.dp))

            if (portrait) {
                PortraitDashboard(
                    snapshot = snapshot,
                    hotspotActive = hotspotActive,
                    hotspotBand = hotspotBand,
                    monthlyLimitBytes = monthlyLimitBytes,
                    monthProgress = monthProgress,
                    threeDayProgress = threeDayProgress,
                    signalProgress = signalProgress,
                    oledSaver = oledSaver,
                    onNavigate = onNavigate,
                    onRequestPhonePermission = onRequestPhonePermission,
                    onRequestUsageAccess = onRequestUsageAccess,
                )
            } else {
                LandscapeDashboard(
                    snapshot = snapshot,
                    hotspotActive = hotspotActive,
                    hotspotBand = hotspotBand,
                    monthlyLimitBytes = monthlyLimitBytes,
                    monthProgress = monthProgress,
                    threeDayProgress = threeDayProgress,
                    signalProgress = signalProgress,
                    oledSaver = oledSaver,
                    onNavigate = onNavigate,
                    onRequestPhonePermission = onRequestPhonePermission,
                    onRequestUsageAccess = onRequestUsageAccess,
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
    onAccentThemeChange: (AccentTheme) -> Unit,
    onDarkModeChange: (Boolean) -> Unit,
    onSettings: () -> Unit,
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
                fontSize = if (portrait) 27.sp else 26.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp,
            )
            Text(
                text = "MOBILE WI-FI",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.6.sp,
            )
        }

        Spacer(Modifier.weight(1f))

        if (!portrait) {
            StatusPill(
                icon = Icons.Rounded.SignalCellularAlt,
                text = if (snapshot.validatedInternet) "Online" else "Offline",
            )
            Spacer(Modifier.width(6.dp))
        }

        StatusPill(
            icon = Icons.Rounded.BatteryFull,
            text = buildString {
                append(snapshot.batteryPercent?.let { "$it%" } ?: "—")
                if (!portrait && snapshot.batteryTimeRemainingText.isNotBlank()) {
                    append(" · ")
                    append(snapshot.batteryTimeRemainingText)
                }
            },
        )
        Spacer(Modifier.width(6.dp))

        Box {
            CircleHeaderButton(
                icon = Icons.Rounded.DarkMode,
                tint = accentTheme.accent,
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
                            Box(Modifier.size(11.dp).background(theme.accent, CircleShape))
                        },
                        onClick = {
                            onAccentThemeChange(theme)
                            themeMenuOpen = false
                        },
                    )
                }
                DropdownMenuItem(
                    text = { Text(if (darkMode) "Use light mode" else "Use dark mode") },
                    onClick = {
                        onDarkModeChange(!darkMode)
                        themeMenuOpen = false
                    },
                )
            }
        }
        Spacer(Modifier.width(5.dp))
        CircleHeaderButton(Icons.Rounded.Settings, MaterialTheme.colorScheme.onSurfaceVariant, onSettings)
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
    oledSaver: Boolean,
    onNavigate: (AppScreen) -> Unit,
    onRequestPhonePermission: () -> Unit,
    onRequestUsageAccess: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        RouterPanel(oledSaver = oledSaver) {
            SignalHero(
                snapshot = snapshot,
                progress = signalProgress,
                size = 178.dp,
                onClick = if (snapshot.phonePermissionGranted) null else onRequestPhonePermission,
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                DataStatus(
                    modifier = Modifier.weight(1f),
                    label = "This month",
                    value = snapshot.monthBytes?.let(::formatDashboardBytes) ?: "—",
                    detail = if (snapshot.usageAccessGranted) {
                        "of ${formatDashboardBytes(monthlyLimitBytes)}"
                    } else {
                        "Tap to allow usage access"
                    },
                    progress = monthProgress,
                    size = 108.dp,
                    onClick = if (snapshot.usageAccessGranted) null else onRequestUsageAccess,
                )
                DataStatus(
                    modifier = Modifier.weight(1f),
                    label = "Last 3 days",
                    value = snapshot.threeDayBytes?.let(::formatDashboardBytes) ?: "—",
                    detail = if (snapshot.usageAccessGranted) "Mobile data" else "Tap to allow usage access",
                    progress = threeDayProgress,
                    size = 108.dp,
                    onClick = if (snapshot.usageAccessGranted) null else onRequestUsageAccess,
                )
            }

            CleanDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                UtilityTile(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.Wifi,
                    label = "Wi-Fi",
                    value = when (hotspotActive) {
                        true -> "Hotspot on"
                        false -> "Hotspot off"
                        null -> "Hotspot"
                    },
                    onClick = { onNavigate(AppScreen.HOTSPOT) },
                )
                UtilityTile(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.SwapVert,
                    label = "Network",
                    value = hotspotBand.ifBlank { "System" },
                    onClick = { onNavigate(AppScreen.DATA) },
                )
            }
            Spacer(Modifier.height(9.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                UtilityTile(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.PowerSettingsNew,
                    label = "Power saving",
                    value = "Battery controls",
                    onClick = { onNavigate(AppScreen.SETTINGS) },
                )
                UtilityTile(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.Brightness6,
                    label = "Display",
                    value = "Screen controls",
                    onClick = { onNavigate(AppScreen.DISPLAY) },
                )
            }

            CleanDivider()

            DeviceFooter(
                snapshot = snapshot,
                hotspotActive = hotspotActive,
                onClick = { onNavigate(AppScreen.DEVICES) },
            )
        }

        Spacer(Modifier.height(10.dp))
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
    oledSaver: Boolean,
    onNavigate: (AppScreen) -> Unit,
    onRequestPhonePermission: () -> Unit,
    onRequestUsageAccess: () -> Unit,
) {
    RouterPanel(
        modifier = Modifier.weight(1f).fillMaxWidth(),
        oledSaver = oledSaver,
    ) {
        Row(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Column(
                modifier = Modifier.weight(0.9f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DataStatus(
                    modifier = Modifier.fillMaxWidth(),
                    label = "This month",
                    value = snapshot.monthBytes?.let(::formatDashboardBytes) ?: "—",
                    detail = if (snapshot.usageAccessGranted) {
                        "of ${formatDashboardBytes(monthlyLimitBytes)}"
                    } else {
                        "Usage access needed"
                    },
                    progress = monthProgress,
                    size = 104.dp,
                    onClick = if (snapshot.usageAccessGranted) null else onRequestUsageAccess,
                )
                DataStatus(
                    modifier = Modifier.fillMaxWidth(),
                    label = "Last 3 days",
                    value = snapshot.threeDayBytes?.let(::formatDashboardBytes) ?: "—",
                    detail = if (snapshot.usageAccessGranted) "Mobile data" else "Usage access needed",
                    progress = threeDayProgress,
                    size = 104.dp,
                    onClick = if (snapshot.usageAccessGranted) null else onRequestUsageAccess,
                )
            }

            Box(
                modifier = Modifier.weight(1.15f),
                contentAlignment = Alignment.Center,
            ) {
                SignalHero(
                    snapshot = snapshot,
                    progress = signalProgress,
                    size = 170.dp,
                    onClick = if (snapshot.phonePermissionGranted) null else onRequestPhonePermission,
                )
            }

            Column(
                modifier = Modifier.weight(1.2f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    UtilityTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.Wifi,
                        label = "Wi-Fi",
                        value = when (hotspotActive) {
                            true -> "Hotspot on"
                            false -> "Hotspot off"
                            null -> "Hotspot"
                        },
                        onClick = { onNavigate(AppScreen.HOTSPOT) },
                    )
                    UtilityTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.SwapVert,
                        label = "Network",
                        value = hotspotBand.ifBlank { "System" },
                        onClick = { onNavigate(AppScreen.DATA) },
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    UtilityTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.PowerSettingsNew,
                        label = "Power saving",
                        value = "Battery controls",
                        onClick = { onNavigate(AppScreen.SETTINGS) },
                    )
                    UtilityTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.Brightness6,
                        label = "Display",
                        value = "Screen controls",
                        onClick = { onNavigate(AppScreen.DISPLAY) },
                    )
                }
            }
        }

        CleanDivider()

        DeviceFooter(
            snapshot = snapshot,
            hotspotActive = hotspotActive,
            onClick = { onNavigate(AppScreen.DEVICES) },
        )
    }
}

@Composable
private fun RouterPanel(
    modifier: Modifier = Modifier,
    oledSaver: Boolean,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(28.dp)
    val accent = MaterialTheme.colorScheme.primary
    Column(
        modifier = modifier
            .shadow(
                elevation = if (oledSaver) 0.dp else 5.dp,
                shape = shape,
                ambientColor = accent.copy(alpha = 0.035f),
                spotColor = Color.Black.copy(alpha = 0.06f),
            )
            .background(MaterialTheme.colorScheme.surface, shape)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.22f), shape)
            .padding(16.dp),
        content = content,
    )
}

@Composable
private fun SignalHero(
    snapshot: SystemSnapshot,
    progress: Float?,
    size: Dp,
    onClick: (() -> Unit)?,
) {
    val modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Mobile network",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(6.dp))
        Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
            RingMeter(progress = progress, size = size, strokeFraction = 0.052f)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Rounded.SignalCellularAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(23.dp),
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = snapshot.networkType.ifBlank { "—" },
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    text = when {
                        !snapshot.phonePermissionGranted -> "Tap to allow phone status"
                        snapshot.signalDbm != null -> "${snapshot.signalDbm} dBm"
                        snapshot.signalLevel != null -> "Signal ${snapshot.signalLevel}/4"
                        snapshot.carrierName.isNotBlank() -> snapshot.carrierName
                        else -> "Signal unavailable"
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                )
            }
        }
        if (snapshot.carrierName.isNotBlank() && snapshot.phonePermissionGranted) {
            Spacer(Modifier.height(3.dp))
            Text(
                text = snapshot.carrierName,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun DataStatus(
    modifier: Modifier,
    label: String,
    value: String,
    detail: String,
    progress: Float?,
    size: Dp,
    onClick: (() -> Unit)?,
) {
    val click = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Column(
        modifier = modifier.then(click),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(4.dp))
        Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
            RingMeter(progress = progress, size = size, strokeFraction = 0.047f)
            Column(
                modifier = Modifier.padding(horizontal = 11.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = value,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = detail,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 7.sp,
                    lineHeight = 9.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun RingMeter(progress: Float?, size: Dp, strokeFraction: Float) {
    val target = progress?.coerceIn(0f, 1f) ?: 0f
    val animated by animateFloatAsState(targetValue = target, label = "zerofiRing")
    val accent = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f)

    Canvas(modifier = Modifier.size(size)) {
        val stroke = Stroke(width = size.toPx() * strokeFraction, cap = StrokeCap.Round)
        drawArc(
            color = track,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            style = stroke,
        )
        drawArc(
            color = if (progress == null) accent.copy(alpha = 0.32f) else accent,
            startAngle = -90f,
            sweepAngle = if (progress == null) 34f else animated * 360f,
            useCenter = false,
            style = stroke,
        )
    }
}

@Composable
private fun UtilityTile(
    modifier: Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.975f else 1f, label = "utilityPress")
    val shape = RoundedCornerShape(18.dp)

    Row(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f), shape)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f), shape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = value,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 8.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun DeviceFooter(
    snapshot: SystemSnapshot,
    hotspotActive: Boolean?,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.09f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Devices,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Connected devices",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = when (hotspotActive) {
                    true -> "Hotspot active · tap for client details"
                    false -> "Hotspot is off"
                    null -> "Tap for hotspot client details"
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 8.sp,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "↓ ${snapshot.downloadBytesPerSecond?.let(::formatRate) ?: "—"}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 8.sp,
            )
            Text(
                text = "↑ ${snapshot.uploadBytesPerSecond?.let(::formatRate) ?: "—"}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 8.sp,
            )
        }
    }
}

@Composable
private fun StatusPill(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f), RoundedCornerShape(14.dp))
            .padding(horizontal = 9.dp, vertical = 6.dp),
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
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}

@Composable
private fun CircleHeaderButton(icon: ImageVector, tint: Color, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.94f else 1f, label = "headerButton")
    Box(
        modifier = Modifier
            .size(34.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .background(MaterialTheme.colorScheme.surface, CircleShape)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f), CircleShape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(17.dp))
    }
}

@Composable
private fun CleanDivider() {
    Spacer(Modifier.height(12.dp))
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
    )
    Spacer(Modifier.height(12.dp))
}

private fun formatDashboardBytes(bytes: Long): String {
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
