package com.zer0spce.zerofi.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zer0spce.zerofi.platform.SystemSnapshot

@Composable
fun CellularDiagnosticsScreen(snapshot: SystemSnapshot, onBack: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp)).clickable(onClick = onBack).padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.ArrowBack, "Back", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(17.dp)); Spacer(Modifier.width(6.dp)); Text("Back", color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.width(10.dp))
            Column { Text("Cellular diagnostics", color = MaterialTheme.colorScheme.onBackground, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold); Text("5G / LTE radio details", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp) }
        }
        Spacer(Modifier.height(10.dp))
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            CellularCard {
                Text(snapshot.networkType, color = MaterialTheme.colorScheme.primary, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
                Text(snapshot.radioMode, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(snapshot.signalQuality, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
            }
            CellularCard {
                CellularLine("Carrier", snapshot.carrierName)
                CellularLine("Displayed network", snapshot.networkType)
                CellularLine("Radio mode", snapshot.radioMode)
                CellularLine("Primary signal", snapshot.signalDbm?.let { "$it dBm" } ?: "Unavailable")
                CellularLine("RSRP", snapshot.signalRsrp?.let { "$it dBm" } ?: "Unavailable")
                CellularLine("RSRQ", snapshot.signalRsrq?.let { "$it dB" } ?: "Unavailable")
                CellularLine("SINR", snapshot.signalSinr?.let { "$it dB" } ?: "Unavailable")
                CellularLine("Signal quality", snapshot.signalQuality)
            }
            CellularCard {
                Text("How to read it", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Spacer(Modifier.height(7.dp))
                Text("RSRP measures received cellular power, RSRQ reflects LTE/NR quality, and SINR shows how clean the usable signal is compared with interference. Some phones and carriers hide one or more values; ZeroFi leaves those as unavailable instead of guessing.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, lineHeight = 13.sp)
            }
        }
    }
}

@Composable private fun CellularCard(content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, shape).border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .14f), shape).padding(14.dp), content = content)
}

@Composable private fun CellularLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, modifier = Modifier.weight(1f))
        Text(value, color = MaterialTheme.colorScheme.onSurface, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
    }
}
