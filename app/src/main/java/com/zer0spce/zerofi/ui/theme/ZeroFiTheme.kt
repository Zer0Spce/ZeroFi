package com.zer0spce.zerofi.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Accent themes intentionally mirror ZeroFi's desktop-style visual language. */
enum class AccentTheme(
    val label: String,
    val accent: Color,
    val accentSoft: Color,
) {
    SUNSET_ORANGE("Sunset Orange", Color(0xFFFF6B2C), Color(0xFFFFA12C)),
    GREEN("Green", Color(0xFF35D07F), Color(0xFF6EE7A8)),
    BLUE("Blue", Color(0xFF45A3FF), Color(0xFF6FC7FF)),
    RED("Red", Color(0xFFFF5353), Color(0xFFFF8585)),
    PURPLE("Purple", Color(0xFF9E6BFF), Color(0xFFC09CFF)),
    MONO("White / Black", Color(0xFFF4F5F7), Color(0xFFC7CBD1)),
}

val ZeroFiBackground = Color(0xFF090B0F)
val ZeroFiSurface = Color(0xFF10151B)
val ZeroFiSurfaceRaised = Color(0xFF151B22)
val ZeroFiBorder = Color(0xFF27313C)
val ZeroFiText = Color(0xFFF3F5F7)
val ZeroFiTextMuted = Color(0xFF97A3B1)

@Composable
fun ZeroFiTheme(
    accentTheme: AccentTheme,
    darkMode: Boolean,
    content: @Composable () -> Unit,
) {
    val accent = accentTheme.accent
    val colors = if (darkMode) {
        darkColorScheme(
            primary = accent,
            secondary = accentTheme.accentSoft,
            background = ZeroFiBackground,
            surface = ZeroFiSurface,
            surfaceVariant = ZeroFiSurfaceRaised,
            outline = ZeroFiBorder,
            onPrimary = Color.Black,
            onBackground = ZeroFiText,
            onSurface = ZeroFiText,
            onSurfaceVariant = ZeroFiTextMuted,
        )
    } else {
        lightColorScheme(
            primary = accent,
            secondary = accentTheme.accentSoft,
            background = Color(0xFFF3F5F7),
            surface = Color.White,
            surfaceVariant = Color(0xFFE9EDF2),
            outline = Color(0xFFCBD2DA),
            onPrimary = Color.Black,
            onBackground = Color(0xFF101419),
            onSurface = Color(0xFF101419),
            onSurfaceVariant = Color(0xFF586472),
        )
    }

    MaterialTheme(
        colorScheme = colors,
        content = content,
    )
}
