package com.zer0spce.zerofi.ui.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

// Kept for preference compatibility; the SCR01 UI intentionally uses one system accent.
enum class AccentTheme(val label: String, val accent: Color, val accentSoft: Color) {
    SUNSET_ORANGE("Default", Color(0xFF3D7DFF), Color(0xFF78A5FF)),
    GREEN("Default", Color(0xFF3D7DFF), Color(0xFF78A5FF)),
    BLUE("Default", Color(0xFF3D7DFF), Color(0xFF78A5FF)),
    RED("Default", Color(0xFF3D7DFF), Color(0xFF78A5FF)),
    PURPLE("Default", Color(0xFF3D7DFF), Color(0xFF78A5FF)),
    MONO("Default", Color(0xFF3D7DFF), Color(0xFF78A5FF)),
}

val ZeroFiBackground = Color(0xFF0B0C0E)
val ZeroFiSurface = Color(0xFF15171A)
val ZeroFiSurfaceRaised = Color(0xFF202328)
val ZeroFiBorder = Color(0xFF34383F)
val ZeroFiText = Color(0xFFF7F7F8)
val ZeroFiTextMuted = Color(0xFFA7ABB2)
val OledBlack = Color.Black
val OledSurface = Color(0xFF080808)
val OledSurfaceRaised = Color(0xFF111111)
val OledBorder = Color(0xFF262626)
val OledTextMuted = Color(0xFF9A9A9A)

@Composable
fun ZeroFiTheme(accentTheme: AccentTheme, darkMode: Boolean, oledSaver: Boolean = false, content: @Composable () -> Unit) {
    val blue = Color(0xFF3D7DFF)
    val blueSoft = Color(0xFF78A5FF)
    val colors = if (darkMode || oledSaver) {
        darkColorScheme(
            primary = blue,
            onPrimary = Color.White,
            primaryContainer = Color(0xFF1C315B),
            onPrimaryContainer = Color(0xFFDCE6FF),
            secondary = blueSoft,
            onSecondary = Color(0xFF081A36),
            secondaryContainer = Color(0xFF28334A),
            onSecondaryContainer = Color(0xFFE5EAFF),
            tertiaryContainer = Color(0xFF243A35),
            onTertiaryContainer = Color(0xFFD9FFF1),
            background = if (oledSaver) Color.Black else ZeroFiBackground,
            surface = if (oledSaver) OledSurface else ZeroFiSurface,
            surfaceVariant = if (oledSaver) OledSurfaceRaised else ZeroFiSurfaceRaised,
            outline = if (oledSaver) OledBorder else ZeroFiBorder,
            onBackground = ZeroFiText,
            onSurface = ZeroFiText,
            onSurfaceVariant = if (oledSaver) OledTextMuted else ZeroFiTextMuted,
        )
    } else {
        lightColorScheme(
            primary = blue,
            onPrimary = Color.White,
            primaryContainer = Color(0xFFDCE6FF),
            onPrimaryContainer = Color(0xFF102550),
            secondary = blueSoft,
            onSecondary = Color(0xFF0B2447),
            secondaryContainer = Color(0xFFE2E9F8),
            onSecondaryContainer = Color(0xFF20283A),
            tertiaryContainer = Color(0xFFDDF5EE),
            onTertiaryContainer = Color(0xFF16342D),
            background = Color(0xFFF5F6F8),
            surface = Color.White,
            surfaceVariant = Color(0xFFE9EBEF),
            outline = Color(0xFFD1D5DB),
            onBackground = Color(0xFF17191C),
            onSurface = Color(0xFF17191C),
            onSurfaceVariant = Color(0xFF686D75),
        )
    }
    MaterialTheme(colorScheme = colors) {
        CompositionLocalProvider(LocalContentColor provides colors.onBackground) {
            content()
        }
    }
}
