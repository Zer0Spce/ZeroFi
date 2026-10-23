package com.zer0spce.zerofi.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DataUsage
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zer0spce.zerofi.platform.RouterFeatureSnapshot
import com.zer0spce.zerofi.platform.SystemSnapshot
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.roundToInt

@Composable
fun DataUsageProScreen(
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
    val limit = (settings.dataLimitGb * GIB).toLong().coerceAtLeast(1L)
    val remaining = (limit - used).coerceAtLeast(0L)
    val percent = ((used * 100.0 / limit).coerceIn(0.0, 999.0)).roundToInt()
    val seven = feature.sevenDayBytes
    val avgPerDay = seven?.takeIf { it > 0 }?.div(7.0)
    val runwayDays = avgPerDay?.takeIf { it > 0 }?.let { ceil(remaining / it).toInt().coerceAtMost(999) }
    var limitValue by remember(settings.dataLimitGb) { mutableFloatStateOf(settings.dataLimitGb.coerceIn(1f, 500f)) }
    var cycle by remember(settings.cycleResetDay) { mutableFloatStateOf(settings.cycleResetDay.toFloat()) }

    ProPage("Data usage", "30-day history, allowance and alerts", onBack) {
        ProCard {
            Text("$percent% of allowance", fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(7.dp))
            LinearProgressIndicator(progress = { (used.toFloat() / limit).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DataMetric(Modifier.weight(1f), "Used", proBytes(used))
                DataMetric(Modifier.weight(1f), "Remaining", proBytes(remaining))
                DataMetric(Modifier.weight(1f), "Runway", runwayDays?.let { "$it days" } ?: "—")
            }
            Spacer(Modifier.height(8.dp))
            ProLine("Today", feature.todayBytes?.let(::proBytes) ?: "Unavailable")
            ProLine("Last 7 days", feature.sevenDayBytes?.let(::proBytes) ?: "Unavailable")
            ProLine("Last 3 days", snapshot.threeDayBytes?.let(::proBytes) ?: "Unavailable")
            ProLine("Daily average", avgPerDay?.toLong()?.let(::proBytes) ?: "Unavailable")
        }

        ProCard {
            Text("30-day mobile usage", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text("Each bar is one day. History refreshes periodically while Usage Access is granted.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp)
            Spacer(Modifier.height(10.dp))
            MonthlyBars(feature.dailyBytes30)
        }

        ProCard {
            Text("Monthly allowance: ${limitValue.roundToInt()} GB", fontWeight = FontWeight.Bold)
            Slider(limitValue, { limitValue = it; onLimitChanged(it) }, valueRange = 1f..500f)
            Text("Cycle reset day: ${cycle.roundToInt()}", fontWeight = FontWeight.Bold)
            Slider(cycle, { cycle = it; onCycleDayChanged(it.roundToInt().coerceIn(1, 28)) }, valueRange = 1f..28f, steps = 26)
        }

        ProCard {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Data alerts", fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                    Text("50%, 75%, 90% and 100% allowance warnings", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp)
                }
                Switch(settings.dataAlertsEnabled, onAlertsChanged)
            }
            Spacer(Modifier.height(7.dp))
            ProLine("Notification permission", if (notificationsGranted) "Granted" else "Not granted")
            if (!notificationsGranted) {
                Spacer(Modifier.height(7.dp)); OutlinedButton(onClick = onRequestNotifications, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Rounded.Notifications, null); Spacer(Modifier.width(6.dp)); Text("Allow notifications") }
            }
            Spacer(Modifier.height(7.dp)); OutlinedButton(onClick = onOpenSystemData, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Rounded.OpenInNew, null); Spacer(Modifier.width(6.dp)); Text("Android data usage") }
            if (!snapshot.usageAccessGranted) {
                Spacer(Modifier.height(7.dp)); Button(onClick = onOpenUsageAccess, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Rounded.DataUsage, null); Spacer(Modifier.width(6.dp)); Text("Grant Usage Access") }
            }
        }
        Spacer(Modifier.height(5.dp))
    }
}

@Composable private fun MonthlyBars(values: List<Long?>) {
    val usable = values.map { it ?: 0L }
    if (usable.isEmpty() || usable.maxOrNull() == 0L) {
        Box(Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) { Text("No usage history available yet", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp) }
        return
    }
    val maxValue = usable.maxOrNull()?.coerceAtLeast(1L) ?: 1L
    val barColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = .16f)
    Canvas(Modifier.fillMaxWidth().height(160.dp)) {
        repeat(4) { index ->
            val y = size.height * index / 3f
            drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        }
        val count = usable.size
        val slot = size.width / count
        val stroke = (slot * .55f).coerceAtLeast(2f)
        usable.forEachIndexed { index, value ->
            val height = size.height * (value.toFloat() / maxValue.toFloat()).coerceIn(0f, 1f)
            val x = slot * index + slot / 2f
            drawLine(barColor, Offset(x, size.height), Offset(x, size.height - height), strokeWidth = stroke)
        }
    }
    Row(Modifier.fillMaxWidth()) {
        Text("30 days ago", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 7.sp)
        Spacer(Modifier.weight(1f))
        Text("Today", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 7.sp)
    }
}

@Composable private fun DataMetric(modifier: Modifier, label: String, value: String) {
    Column(modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .48f), RoundedCornerShape(12.dp)).padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 7.sp)
        Text(value, fontWeight = FontWeight.Bold, fontSize = 10.sp, maxLines = 1)
    }
}

@Composable private fun ProPage(title: String, subtitle: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top)).padding(horizontal = 14.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← Back") }
            Spacer(Modifier.width(6.dp)); Column { Text(title, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold); Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp) }
        }
        Spacer(Modifier.height(8.dp)); Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

@Composable private fun ProCard(content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, shape).border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .14f), shape).padding(14.dp), content = content)
}

@Composable private fun ProLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) { Text(label, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp); Text(value, fontWeight = FontWeight.SemiBold, fontSize = 9.sp) }
}

private fun proBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format(Locale.US, "%.0f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format(Locale.US, "%.1f MB", mb)
    return String.format(Locale.US, "%.2f GB", mb / 1024.0)
}

private const val GIB = 1024L * 1024L * 1024L
