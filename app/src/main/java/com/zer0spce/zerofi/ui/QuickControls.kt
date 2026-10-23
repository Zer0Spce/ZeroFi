package com.zer0spce.zerofi.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BoxScope.HomeLockButton(locked: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(17.dp)
    Row(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(top = 68.dp, end = 16.dp)
            .shadow(4.dp, shape)
            .background(MaterialTheme.colorScheme.surface, shape)
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = if (locked) .55f else .20f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Icon(
            if (locked) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
            contentDescription = if (locked) "Unlock ZeroFi" else "Lock ZeroFi",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
        Text(
            if (locked) "UNLOCK" else "LOCK",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun BoxScope.HotspotAutomationSettingsButton(
    enabled: Boolean,
    configured: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(18.dp)
            .fillMaxWidth()
            .shadow(5.dp, shape)
            .background(MaterialTheme.colorScheme.surface, shape)
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .28f), shape)
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Settings, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text("Hotspot automation", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text(
                    when {
                        enabled && configured -> "Configured · tap to disable"
                        enabled -> "Setup required · tap to configure Accessibility"
                        else -> "Off · tap to configure automatic hotspot"
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.sp,
                )
            }
            Text(
                if (enabled && configured) "ON" else "SET UP",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
            )
        }
    }
}
