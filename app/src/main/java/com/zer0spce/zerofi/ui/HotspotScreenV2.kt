package com.zer0spce.zerofi.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HotspotScreenV2(
    active: Boolean?,
    detectedBand: String?,
    liveSsid: String?,
    liveSecurity: String?,
    ssid: String,
    password: String,
    security: String,
    preferredBand: String,
    profileConfirmed: Boolean,
    automationReady: Boolean,
    onBack: () -> Unit,
    onToggleHotspot: () -> Unit,
    onOpenSystemHotspot: () -> Unit,
    onApplyProfile: (String, String, String, String) -> Unit,
) {
    val context = LocalContext.current
    var localSsid by remember(ssid) { mutableStateOf(ssid) }
    var localPassword by remember(password) { mutableStateOf(password) }
    var localSecurity by remember(security) { mutableStateOf(security) }
    var localBand by remember(preferredBand) { mutableStateOf(preferredBand) }
    var showPassword by remember { mutableStateOf(false) }
    val confirmed = profileConfirmed && localSsid == ssid && localPassword == password && localSecurity == security
    val qrValid = confirmed && localSsid.isNotBlank() && (localSecurity.equals("nopass", true) || localPassword.length in 8..63)
    val qr = remember(localSsid, localPassword, localSecurity, qrValid) {
        if (qrValid) generateQrBitmap(wifiQrPayload(localSsid, localPassword, localSecurity), 360) else null
    }

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, null); Spacer(Modifier.width(5.dp)); Text("Back") }
            Spacer(Modifier.width(6.dp))
            Column { Text("Wi-Fi", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold); Text("Pocket Wi-Fi connection", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        Spacer(Modifier.height(8.dp))
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            HotspotV2Card {
                Text(liveSsid ?: localSsid.ifBlank { "ZeroFi" }, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                Text(
                    when (active) { true -> "HOTSPOT ON"; false -> "HOTSPOT OFF"; null -> "SYSTEM MANAGED" },
                    modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                    color = if (active == true) Color(0xFF35D97B) else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp, fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(10.dp))
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    if (qr != null) {
                        Image(qr.asImageBitmap(), "Wi-Fi QR", Modifier.size(250.dp).background(Color.White, RoundedCornerShape(20.dp)).padding(12.dp))
                    } else {
                        Box(Modifier.size(250.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp)), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Rounded.QrCode2, null, Modifier.size(46.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(10.dp))
                                Text("Apply and verify this hotspot profile to unlock QR", textAlign = TextAlign.Center, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 24.dp))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onToggleHotspot, modifier = Modifier.weight(1f)) { Icon(Icons.Rounded.PowerSettingsNew, null); Spacer(Modifier.width(6.dp)); Text(if (active == true) "Turn hotspot off" else "Turn hotspot on") }
                    OutlinedButton(onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("ZeroFi hotspot password", localPassword))
                    }, enabled = localPassword.isNotBlank(), modifier = Modifier.weight(1f)) { Icon(Icons.Rounded.ContentCopy, null); Spacer(Modifier.width(6.dp)); Text("Copy password") }
                }
                Spacer(Modifier.height(6.dp))
                HotspotV2Line("Live hotspot", liveSsid ?: "Not exposed by Android")
                HotspotV2Line("Band", detectedBand ?: "System")
                HotspotV2Line("Security", liveSecurity ?: localSecurity)
                HotspotV2Line("QR", if (qrValid) "Ready" else "Locked until verified")
            }

            HotspotV2Card {
                Text("ANDROID HOTSPOT PROFILE", color = MaterialTheme.colorScheme.primary, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(localSsid, { localSsid = it.take(32) }, label = { Text("Hotspot name (SSID)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(7.dp))
                OutlinedTextField(
                    localPassword, { localPassword = it.take(63) }, label = { Text("Hotspot password") }, singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = { IconButton(onClick = { showPassword = !showPassword }) { Icon(if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, null) } },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(9.dp))
                Text("Security", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    listOf("WPA", "nopass").forEach { option -> FilterChip(selected = localSecurity == option, onClick = { localSecurity = option }, label = { Text(option) }) }
                }
                Text("Preferred band", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    listOf("System", "2.4 GHz", "5 GHz").forEach { option -> FilterChip(selected = localBand == option, onClick = { localBand = option }, label = { Text(option) }) }
                }
                Spacer(Modifier.height(8.dp))
                Text(if (automationReady) "Hotspot Automation is ready to apply these values to Android." else "Enable Hotspot Automation to change the real Android tethering profile.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                Spacer(Modifier.height(10.dp))
                Button(onClick = { onApplyProfile(localSsid, localPassword, localSecurity, localBand) }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Rounded.Wifi, null); Spacer(Modifier.width(7.dp)); Text("Apply to Android Hotspot") }
                Spacer(Modifier.height(7.dp))
                OutlinedButton(onClick = onOpenSystemHotspot, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Rounded.OpenInNew, null); Spacer(Modifier.width(7.dp)); Text("Open Android Hotspot Settings") }
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable private fun HotspotV2Card(content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(19.dp)
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, shape).border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .14f), shape).padding(14.dp), content = content)
}

@Composable private fun HotspotV2Line(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) { Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, modifier = Modifier.weight(1f)); Text(value, fontSize = 9.sp, fontWeight = FontWeight.SemiBold) }
}
