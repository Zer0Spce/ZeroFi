package com.zer0spce.zerofi.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.zer0spce.zerofi.ui.theme.AccentTheme
import com.zer0spce.zerofi.ui.theme.ZeroFiTheme

@Composable
fun ZeroFiApp() {
    var accentTheme by rememberSaveable { mutableStateOf(AccentTheme.SUNSET_ORANGE) }
    var darkMode by rememberSaveable { mutableStateOf(true) }

    ZeroFiTheme(
        accentTheme = accentTheme,
        darkMode = darkMode,
    ) {
        DashboardScreen(
            accentTheme = accentTheme,
            darkMode = darkMode,
            onAccentThemeChange = { accentTheme = it },
            onDarkModeChange = { darkMode = it },
        )
    }
}
