package com.zer0spce.zerofi.platform

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.util.ArrayDeque

/**
 * Optional stock-Android fallback for devices where a normal app cannot change
 * Internet tethering directly. After the user enables this service once, ZeroFi
 * opens Quick Settings, inspects the Hotspot tile, taps it only when it can
 * positively identify the OFF state, then returns to ZeroFi.
 *
 * It deliberately never blind-toggles a tile whose state cannot be determined.
 */
class HotspotAutomationService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private var pending = false
    private var attempts = 0

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!pending) return
        val packageName = event?.packageName?.toString().orEmpty()
        if (packageName.isEmpty() || packageName.contains("systemui", ignoreCase = true)) {
            scheduleScan(120L)
        }
    }

    override fun onInterrupt() = Unit

    private fun requestEnsureOn() {
        if (pending) return
        pending = true
        attempts = 0
        performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
        scheduleScan(650L)
    }

    private fun scheduleScan(delayMs: Long) {
        handler.postDelayed({ scanQuickSettings() }, delayMs)
    }

    private fun scanQuickSettings() {
        if (!pending) return
        attempts++
        val root = rootInActiveWindow
        val tile = root?.let(::findHotspotNode)

        if (tile != null) {
            when (readTileState(tile)) {
                TileState.ON -> finishAndReturn()
                TileState.OFF -> {
                    if (clickNodeOrParent(tile)) {
                        handler.postDelayed({ finishAndReturn() }, 700L)
                    } else {
                        retryOrFinish()
                    }
                }
                TileState.UNKNOWN -> retryOrFinish()
            }
        } else {
            retryOrFinish()
        }
    }

    private fun retryOrFinish() {
        if (attempts < 4) {
            scheduleScan(350L)
        } else {
            finishAndReturn()
        }
    }

    private fun finishAndReturn() {
        if (!pending) return
        pending = false
        attempts = 0
        performGlobalAction(GLOBAL_ACTION_BACK)
    }

    private fun findHotspotNode(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            val label = buildNodeLabel(node).lowercase()
            if (HOTSPOT_LABELS.any { label.contains(it) }) return node
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let(queue::addLast)
            }
        }
        return null
    }

    private fun buildNodeLabel(node: AccessibilityNodeInfo): String = buildString {
        node.text?.let { append(it).append(' ') }
        node.contentDescription?.let { append(it).append(' ') }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            node.stateDescription?.let { append(it).append(' ') }
        }
    }

    private fun readTileState(node: AccessibilityNodeInfo): TileState {
        val label = buildNodeLabel(node).lowercase()
        if (node.isCheckable) return if (node.isChecked) TileState.ON else TileState.OFF

        val offTokens = listOf(" off", "off ", "disabled", "not active", "inactive")
        val onTokens = listOf(" on", "on ", "enabled", "active")
        if (offTokens.any(label::contains)) return TileState.OFF
        if (onTokens.any(label::contains)) return TileState.ON

        var parent = node.parent
        repeat(3) {
            parent ?: return@repeat
            val parentLabel = buildNodeLabel(parent).lowercase()
            if (parent.isCheckable) return if (parent.isChecked) TileState.ON else TileState.OFF
            if (offTokens.any(parentLabel::contains)) return TileState.OFF
            if (onTokens.any(parentLabel::contains)) return TileState.ON
            parent = parent.parent
        }
        return TileState.UNKNOWN
    }

    private fun clickNodeOrParent(node: AccessibilityNodeInfo): Boolean {
        var current: AccessibilityNodeInfo? = node
        repeat(5) {
            val candidate = current ?: return false
            if (candidate.isClickable && candidate.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                return true
            }
            current = candidate.parent
        }
        return false
    }

    private enum class TileState { ON, OFF, UNKNOWN }

    companion object {
        @Volatile
        private var instance: HotspotAutomationService? = null

        private val HOTSPOT_LABELS = listOf(
            "mobile hotspot",
            "wi-fi hotspot",
            "wifi hotspot",
            "personal hotspot",
            "hotspot",
        )

        fun isEnabled(context: Context): Boolean {
            val enabled = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            ).orEmpty()
            val component = ComponentName(context, HotspotAutomationService::class.java).flattenToString()
            return enabled.split(':').any { it.equals(component, ignoreCase = true) }
        }

        fun requestEnsureHotspotOn() {
            val service = instance ?: return
            service.handler.post { service.requestEnsureOn() }
        }
    }
}
