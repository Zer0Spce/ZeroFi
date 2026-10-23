package com.zer0spce.zerofi.ui

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.Modifier

/**
 * Fallback for the adaptive dashboard root where a weight-like fill is needed
 * outside a RowScope/ColumnScope. Real Row/Column weight extensions take
 * precedence inside their respective layout scopes.
 */
@Suppress("UNUSED_PARAMETER")
internal fun Modifier.weight(weight: Float): Modifier = fillMaxHeight()
