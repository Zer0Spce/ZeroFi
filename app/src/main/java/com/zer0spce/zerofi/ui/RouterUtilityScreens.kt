package com.zer0spce.zerofi.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.WifiTethering
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zer0spce.zerofi.platform.SystemSnapshot
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StandbyScreen(snapshot: SystemSnapshot, hotspotActive: Boolean?, onWake: () -> Unit) {
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) { while (true) { now = Date(); delay(1_000L) } }
    val time = remember(now) { SimpleDateFormat("HH:mm", Locale.getDefault()).format(now) }
    val date = remember(now) { SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(now) }
    Box(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).clickable(onClick = onWake).padding(28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(time, color = MaterialTheme.colorScheme.onBackground, fontSize = 58.sp, fontWeight = FontWeight.Light)
            Text(date, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            Spacer(Modifier.height(30.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StandbyPill(Icons.Rounded.SignalCellularAlt, snapshot.networkType)
                StandbyPill(Icons.Rounded.WifiTethering, if (hotspotActive == true) "Hotspot on" else "Hotspot off")
                StandbyPill(Icons.Rounded.BatteryFull, snapshot.batteryPercent?.let { "$it%" } ?: "—")
            }
            Spacer(Modifier.height(18.dp))
            Text(snapshot.carrierName, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(if (snapshot.validatedInternet) "Internet connected" else "Internet unavailable", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
            Spacer(Modifier.height(32.dp))
            Text("Tap to open ZeroFi", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
        }
    }
}

@Composable
private fun StandbyPill(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .55f), RoundedCornerShape(18.dp)).padding(horizontal = 11.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp)); Spacer(Modifier.width(5.dp)); Text(text, color = MaterialTheme.colorScheme.onSurface, fontSize = 9.sp)
    }
}

@Composable
fun DiagnosticsScreen(snapshot: SystemSnapshot, hotspotActive: Boolean?, hotspotBand: String, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)).clickable(onClick = onBack).padding(9.dp)) { Icon(Icons.Rounded.ArrowBack, "Back", tint = MaterialTheme.colorScheme.primary) }
            Spacer(Modifier.width(12.dp)); Column { Text("Router diagnostics", color = MaterialTheme.colorScheme.onBackground, fontSize = 22.sp, fontWeight = FontWeight.Bold); Text("Live ZeroFi device and network status", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp) }
        }
        Spacer(Modifier.height(16.dp))
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val portrait = maxHeight > maxWidth
            if (portrait) Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) { DiagnosticsCard(snapshot, hotspotActive, hotspotBand); PowerCard(snapshot) }
            else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) { Box(Modifier.weight(1f)) { DiagnosticsCard(snapshot, hotspotActive, hotspotBand) }; Box(Modifier.weight(1f)) { PowerCard(snapshot) } }
        }
    }
}

@Composable
private fun DiagnosticsCard(snapshot: SystemSnapshot, hotspotActive: Boolean?, hotspotBand: String) {
    DiagnosticSurface("NETWORK") {
        DiagnosticLine("Internet", if (snapshot.validatedInternet) "Connected" else "Unavailable")
        DiagnosticLine("Carrier", snapshot.carrierName)
        DiagnosticLine("Network", snapshot.networkType)
        DiagnosticLine("Signal", snapshot.signalDbm?.let { "$it dBm" } ?: snapshot.signalLevel?.let { "$it / 4" } ?: "Unavailable")
        DiagnosticLine("Hotspot", when (hotspotActive) { true -> "On"; false -> "Off"; null -> "System managed" })
        DiagnosticLine("Band", hotspotBand)
        DiagnosticLine("Download", snapshot.downloadBytesPerSecond?.let(::formatRate) ?: "—")
        DiagnosticLine("Upload", snapshot.uploadBytesPerSecond?.let(::formatRate) ?: "—")
    }
}

@Composable
private fun PowerCard(snapshot: SystemSnapshot) {
    DiagnosticSurface("DEVICE") {
        DiagnosticLine("Battery", snapshot.batteryPercent?.let { "$it%" } ?: "Unavailable")
        DiagnosticLine("Charging", if (snapshot.charging) "Yes" else "No")
        DiagnosticLine("Temperature", snapshot.batteryTempC?.let { String.format(Locale.US, "%.1f °C", it) } ?: "Unavailable")
        DiagnosticLine("Battery estimate", snapshot.batteryTimeRemainingText)
        DiagnosticLine("Uptime", snapshot.uptimeText)
        DiagnosticLine("Usage access", if (snapshot.usageAccessGranted) "Granted" else "Not granted")
        DiagnosticLine("Phone status", if (snapshot.phonePermissionGranted) "Granted" else "Not granted")
    }
}

@Composable
private fun DiagnosticSurface(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp)).padding(16.dp)) { Text(title, color = MaterialTheme.colorScheme.primary, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp); Spacer(Modifier.height(9.dp)); content() }
}

@Composable
private fun DiagnosticLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) { Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, modifier = Modifier.weight(1f)); Text(value, color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End) }
}
