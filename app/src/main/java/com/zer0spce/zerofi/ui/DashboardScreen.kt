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
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.zer0spce.zerofi.ui.theme.AccentTheme

@Composable
fun DashboardScreen(
    accentTheme: AccentTheme,
    darkMode: Boolean,
    onAccentThemeChange: (AccentTheme) -> Unit,
    onDarkModeChange: (Boolean) -> Unit,
) {
    val accent = MaterialTheme.colorScheme.primary
    val backgroundBrush = Brush.radialGradient(
        colors = listOf(
            accent.copy(alpha = if (darkMode) 0.11f else 0.08f),
            MaterialTheme.colorScheme.background,
        ),
        radius = 1200f,
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush),
    ) {
        val compact = maxHeight < 420.dp || maxWidth < 760.dp
        val outerPadding = if (compact) 12.dp else 20.dp
        val gap = if (compact) 9.dp else 14.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(outerPadding),
        ) {
            DashboardHeader(
                accentTheme = accentTheme,
                darkMode = darkMode,
                compact = compact,
                onAccentThemeChange = onAccentThemeChange,
                onDarkModeChange = onDarkModeChange,
            )

            Spacer(Modifier.height(gap))

            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(gap),
            ) {
                GaugeCard(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    title = "This month",
                    value = "—",
                    detail = "Usage permission needed",
                    progress = null,
                    compact = compact,
                )
                GaugeCard(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    title = "Last 3 days",
                    value = "—",
                    detail = "Mobile data",
                    progress = null,
                    compact = compact,
                )
                GaugeCard(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    title = "Mobile network",
                    value = "—",
                    detail = "Waiting for phone access",
                    progress = null,
                    compact = compact,
                )
                StatusPanel(
                    modifier = Modifier
                        .widthIn(min = if (compact) 160.dp else 205.dp)
                        .fillMaxHeight(),
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
                    subtitle = "Open controls",
                    compact = compact,
                )
                ActionTile(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.Devices,
                    title = "Devices",
                    subtitle = "Clients",
                    compact = compact,
                )
                ActionTile(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.DataUsage,
                    title = "Data",
                    subtitle = "Usage & limits",
                    compact = compact,
                )
                ActionTile(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.Brightness6,
                    title = "Display",
                    subtitle = "Screen & lock",
                    compact = compact,
                )
                ActionTile(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.Settings,
                    title = "Settings",
                    subtitle = "Router mode",
                    compact = compact,
                )
            }
        }
    }
}

@Composable
private fun DashboardHeader(
    accentTheme: AccentTheme,
    darkMode: Boolean,
    compact: Boolean,
    onAccentThemeChange: (AccentTheme) -> Unit,
    onDarkModeChange: (Boolean) -> Unit,
) {
    var themeMenuOpen by remember { mutableStateOf(false) }
    val accent = MaterialTheme.colorScheme.primary

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = "ZeroFi",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = if (compact) 24.sp else 31.sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = "POCKET WI-FI DASHBOARD",
                color = accent,
                fontSize = if (compact) 8.sp else 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.6.sp,
            )
        }

        Spacer(Modifier.weight(1f))

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
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(theme.accent, CircleShape),
                            )
                        },
                        onClick = {
                            onAccentThemeChange(theme)
                            themeMenuOpen = false
                        },
                    )
                }
            }
        }

        Spacer(Modifier.width(8.dp))

        HeaderControl(
            text = if (darkMode) "Dark" else "Light",
            trailing = Icons.Rounded.LightMode,
            compact = compact,
            onClick = { onDarkModeChange(!darkMode) },
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
    val shape = RoundedCornerShape(if (compact) 10.dp else 13.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, label = "headerControlScale")

    Row(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.86f), shape)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.75f), shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(
                horizontal = if (compact) 10.dp else 14.dp,
                vertical = if (compact) 7.dp else 9.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = if (compact) 10.sp else 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
        Icon(
            imageVector = trailing,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(if (compact) 15.dp else 18.dp),
        )
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
) {
    val accent = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(if (compact) 15.dp else 20.dp)

    Column(
        modifier = modifier
            .shadow(
                elevation = 10.dp,
                shape = shape,
                ambientColor = accent.copy(alpha = 0.10f),
                spotColor = accent.copy(alpha = 0.16f),
            )
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.94f), shape)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.60f), shape)
            .padding(if (compact) 10.dp else 15.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = if (compact) 10.sp else 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )

        Gauge(
            value = value,
            progress = progress,
            size = if (compact) 72.dp else 104.dp,
        )

        Text(
            text = detail,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f),
            fontSize = if (compact) 8.sp else 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun Gauge(
    value: String,
    progress: Float?,
    size: Dp,
) {
    val accent = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.outline.copy(alpha = 0.38f)
    val animatedProgress by animateFloatAsState(
        targetValue = progress?.coerceIn(0f, 1f) ?: 0f,
        label = "gaugeProgress",
    )

    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
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
                brush = Brush.sweepGradient(
                    listOf(accent.copy(alpha = 0.55f), accent),
                ),
                startAngle = 135f,
                sweepAngle = 270f * animatedProgress,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
        }

        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = if (size < 90.dp) 18.sp else 26.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun StatusPanel(
    modifier: Modifier,
    compact: Boolean,
) {
    val shape = RoundedCornerShape(if (compact) 15.dp else 20.dp)
    val accent = MaterialTheme.colorScheme.primary

    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.94f), shape)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.60f), shape)
            .padding(if (compact) 10.dp else 14.dp),
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        StatusRow("Network", "Checking…", accent, compact)
        StatusRow("Hotspot", "System", accent, compact)
        StatusRow("Wi-Fi band", "—", accent, compact)
        StatusRow("Connected", "—", accent, compact)
        StatusRow("Battery", "—", accent, compact)
    }
}

@Composable
private fun StatusRow(
    label: String,
    value: String,
    accent: Color,
    compact: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(if (compact) 6.dp else 7.dp)
                .background(accent.copy(alpha = 0.85f), CircleShape),
        )
        Spacer(Modifier.width(if (compact) 6.dp else 8.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = if (compact) 8.sp else 9.sp,
            )
            Text(
                text = value,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = if (compact) 10.sp else 12.sp,
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
    onClick: () -> Unit = {},
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val accent = MaterialTheme.colorScheme.primary
    val scale by animateFloatAsState(if (pressed) 0.965f else 1f, label = "actionTileScale")
    val background by animateColorAsState(
        targetValue = if (pressed) {
            accent.copy(alpha = 0.17f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.76f)
        },
        label = "actionTileColor",
    )
    val shape = RoundedCornerShape(if (compact) 13.dp else 17.dp)

    Row(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = if (pressed) 3.dp else 8.dp,
                shape = shape,
                ambientColor = accent.copy(alpha = if (pressed) 0.06f else 0.11f),
                spotColor = accent.copy(alpha = if (pressed) 0.08f else 0.16f),
            )
            .background(background, shape)
            .border(
                width = 1.dp,
                color = if (pressed) accent.copy(alpha = 0.65f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
                shape = shape,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(
                horizontal = if (compact) 9.dp else 12.dp,
                vertical = if (compact) 8.dp else 11.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (compact) 7.dp else 10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(if (compact) 28.dp else 36.dp)
                .background(accent.copy(alpha = 0.13f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(if (compact) 16.dp else 20.dp),
            )
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = if (compact) 9.sp else 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = if (compact) 7.sp else 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
