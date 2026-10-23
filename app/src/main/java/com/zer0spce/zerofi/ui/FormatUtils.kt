package com.zer0spce.zerofi.ui

import java.util.Locale

val formatBytes: (Long) -> String = { bytes ->
    when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024L * 1024L -> String.format(Locale.US, "%.0f KB", bytes / 1024.0)
        bytes < 1024L * 1024L * 1024L -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
        else -> String.format(Locale.US, "%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
    }
}
