package com.zer0spce.zerofi.ui

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
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zer0spce.zerofi.platform.InternetHealthSnapshot
import com.zer0spce.zerofi.platform.SystemSnapshot
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun InternetHealthScreen(
    health: InternetHealthSnapshot,
    snapshot: SystemSnapshot,
    automationReady: Boolean,
    checking: Boolean,
    onBack: () -> Unit,
    onRetest: () -> Unit,
    onRecover: () -> Unit,
    onOpenMobileSettings: () -> Unit,
) {
    val locale = LocalLocale.current.platformLocale
    val checkedText = if (health.checkedAt > 0) {
        "Last checked ${SimpleDateFormat("HH:mm:ss", locale).format(Date(health.checkedAt))}"
    } else {
        "Run a check to measure the current Internet path"
    }

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
            Column { Text("Internet health", color = MaterialTheme.colorScheme.onBackground, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold); Text("Connectivity checks & safe recovery", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp) }
        }
        Spacer(Modifier.height(10.dp))
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            HealthCard {
                val healthy = health.summary == "Healthy" || health.summary == "Online with minor issues"
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(12.dp).background(if (healthy) Color(0xFF35D97B) else MaterialTheme.colorScheme.error, CircleShape))
                    Spacer(Modifier.width(9.dp))
                    Column(Modifier.weight(1f)) {
                        Text(if (checking) "Checking…" else health.summary, color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                        Text(checkedText, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp)
                    }
                }
            }
            HealthCard {
                HealthLine2("Android validated Internet", snapshot.validatedInternet, if (snapshot.validatedInternet) "Yes" else "No")
                HealthLine2("DNS resolution", health.dnsOk, if (health.dnsOk) "Working" else "Failed")
                HealthLine2("Internet TCP path", health.tcpOk, if (health.tcpOk) "Reachable" else "Failed")
                HealthLine2("HTTPS validation", health.httpOk, if (health.httpOk) "Working" else "Failed")
                HealthValue("Median latency", health.latencyMs?.let { "$it ms" } ?: "Unavailable")
                HealthValue("Probe loss", health.packetLossPercent?.let { "$it%" } ?: "Unavailable")
            }
            HealthCard {
                Button(onClick = onRetest, enabled = !checking, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Rounded.Refresh, null); Spacer(Modifier.width(7.dp)); Text(if (checking) "Checking…" else "Run health check") }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onRecover, enabled = automationReady, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Rounded.Healing, null); Spacer(Modifier.width(7.dp)); Text("Recover router connection") }
                Text(if (automationReady) "Recovery uses ZeroFi's startup sequence: Wi-Fi off → mobile data on → hotspot on." else "Enable ZeroFi Hotspot Automation before using one-tap recovery.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp, modifier = Modifier.padding(top = 7.dp))
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onOpenMobileSettings, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Rounded.SettingsCell, null); Spacer(Modifier.width(7.dp)); Text("Open mobile network settings") }
            }
            HealthCard {
                Text("Recovery is intentionally conservative", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                Text("ZeroFi does not loop airplane mode, reset APNs, or repeatedly tear down a working connection. It only runs the same user-enabled router preparation flow you configured.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.5.sp, lineHeight = 12.sp)
            }
        }
    }
}

@Composable private fun HealthCard(content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, shape).border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .14f), shape).padding(14.dp), content = content)
}

@Composable private fun HealthLine2(label: String, ok: Boolean, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).background(if (ok) Color(0xFF35D97B) else MaterialTheme.colorScheme.error, CircleShape)); Spacer(Modifier.width(7.dp))
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, modifier = Modifier.weight(1f)); Text(value, color = MaterialTheme.colorScheme.onSurface, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable private fun HealthValue(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) { Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, modifier = Modifier.weight(1f)); Text(value, color = MaterialTheme.colorScheme.onSurface, fontSize = 9.sp, fontWeight = FontWeight.SemiBold) }
}
