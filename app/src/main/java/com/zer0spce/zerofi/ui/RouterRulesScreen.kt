package com.zer0spce.zerofi.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.BrightnessLow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zer0spce.zerofi.data.RouterRulesStore
import kotlin.math.roundToInt

@Composable
fun RouterRulesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val store = remember { RouterRulesStore(context) }
    var quietEnabled by remember { mutableStateOf(store.quietHoursEnabled) }
    var startHour by remember { mutableFloatStateOf(store.quietStartHour.toFloat()) }
    var endHour by remember { mutableFloatStateOf(store.quietEndHour.toFloat()) }
    var dimCharging by remember { mutableStateOf(store.dimWhileCharging) }

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← Back") }
            Spacer(Modifier.width(6.dp))
            Column { Text("Automation rules", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold); Text("Pocket Wi-Fi schedules & charging behavior", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        Spacer(Modifier.height(8.dp))
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            RuleCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Bedtime, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) { Text("Quiet hours", fontWeight = FontWeight.Bold, fontSize = 12.sp); Text("Turn the hotspot off overnight and restore it afterward when Hotspot Automation is available.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp) }
                    Switch(quietEnabled, { value -> quietEnabled = value; store.quietHoursEnabled = value })
                }
                Spacer(Modifier.height(10.dp))
                Text("Start: ${formatHour(startHour.roundToInt())}", fontWeight = FontWeight.SemiBold, fontSize = 10.sp)
                Slider(startHour, { startHour = it; store.quietStartHour = it.roundToInt() }, valueRange = 0f..23f, steps = 22, enabled = quietEnabled)
                Text("End: ${formatHour(endHour.roundToInt())}", fontWeight = FontWeight.SemiBold, fontSize = 10.sp)
                Slider(endHour, { endHour = it; store.quietEndHour = it.roundToInt() }, valueRange = 0f..23f, steps = 22, enabled = quietEnabled)
                Text("Schedule enforcement requires ZeroFi to be running. Launch-after-boot is recommended for a dedicated router phone.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp)
            }

            RuleCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.BrightnessLow, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) { Text("Dim while charging", fontWeight = FontWeight.Bold, fontSize = 12.sp); Text("Reduce ZeroFi's app brightness while the phone is charging to lower heat and OLED wear.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp) }
                    Switch(dimCharging, { value -> dimCharging = value; store.dimWhileCharging = value })
                }
            }
            Spacer(Modifier.height(5.dp))
        }
    }
}

@Composable private fun RuleCard(content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, shape).border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .14f), shape).padding(14.dp), content = content)
}

private fun formatHour(hour: Int): String {
    val h = hour.coerceIn(0, 23)
    val suffix = if (h < 12) "AM" else "PM"
    val display = when (val normalized = h % 12) { 0 -> 12; else -> normalized }
    return "$display:00 $suffix"
}
