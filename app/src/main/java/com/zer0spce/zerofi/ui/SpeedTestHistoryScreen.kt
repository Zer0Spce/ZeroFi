package com.zer0spce.zerofi.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zer0spce.zerofi.data.SpeedHistoryEntry
import com.zer0spce.zerofi.data.SpeedHistoryStore
import com.zer0spce.zerofi.platform.SpeedTestEngine
import com.zer0spce.zerofi.platform.SpeedTestResult
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SpeedTestHistoryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val store = remember { SpeedHistoryStore(context) }
    val scope = rememberCoroutineScope()
    var running by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<SpeedTestResult?>(null) }
    var history by remember { mutableStateOf(store.entries()) }

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← Back") }
            Spacer(Modifier.width(6.dp))
            Column { Text("Speed test", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold); Text("Manual test with saved results", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        Spacer(Modifier.height(8.dp))
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            HistoryCard {
                Text("Quick test", fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
                Text("Uses roughly 2 MB download + 512 KB upload. Results are stored only on this device.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SpeedMetric(Modifier.weight(1f), "Ping", result?.latencyMs?.let { "$it ms" } ?: "—")
                    SpeedMetric(Modifier.weight(1f), "Download", result?.downloadMbps?.let { String.format(Locale.US, "%.1f Mbps", it) } ?: "—")
                    SpeedMetric(Modifier.weight(1f), "Upload", result?.uploadMbps?.let { String.format(Locale.US, "%.1f Mbps", it) } ?: "—")
                }
                result?.error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 9.sp, modifier = Modifier.padding(top = 8.dp)) }
                Spacer(Modifier.height(12.dp))
                Button(
                    enabled = !running,
                    onClick = {
                        running = true
                        result = null
                        scope.launch {
                            val tested = SpeedTestEngine.run()
                            result = tested
                            store.add(tested)
                            history = store.entries()
                            running = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (running) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Icon(Icons.Rounded.Speed, null)
                    Spacer(Modifier.width(8.dp)); Text(if (running) "Testing…" else "Start speed test")
                }
            }

            HistoryCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Recent tests", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    if (history.isNotEmpty()) TextButton(onClick = { store.clear(); history = emptyList() }) { Icon(Icons.Rounded.DeleteSweep, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Clear") }
                }
                if (history.isEmpty()) {
                    Text("Run a test to start history.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, modifier = Modifier.padding(vertical = 10.dp))
                } else history.forEach { SpeedHistoryRow(it) }
            }
            Spacer(Modifier.height(5.dp))
        }
    }
}

@Composable private fun SpeedHistoryRow(entry: SpeedHistoryEntry) {
    val date = remember(entry.timestampMs) { SimpleDateFormat("MMM d · HH:mm", Locale.getDefault()).format(Date(entry.timestampMs)) }
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(date, fontSize = 9.sp, fontWeight = FontWeight.SemiBold); Text(entry.latencyMs?.let { "$it ms ping" } ?: "Ping —", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 7.5.sp) }
        Text(entry.downloadMbps?.let { "↓ %.1f".format(Locale.US, it) } ?: "↓ —", fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(12.dp))
        Text(entry.uploadMbps?.let { "↑ %.1f".format(Locale.US, it) } ?: "↑ —", fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .12f))
}

@Composable private fun SpeedMetric(modifier: Modifier, label: String, value: String) {
    Column(modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .48f), RoundedCornerShape(12.dp)).padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 7.sp)
        Text(value, fontWeight = FontWeight.Bold, fontSize = 10.sp, maxLines = 1)
    }
}

@Composable private fun HistoryCard(content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, shape).border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .14f), shape).padding(14.dp), content = content)
}
