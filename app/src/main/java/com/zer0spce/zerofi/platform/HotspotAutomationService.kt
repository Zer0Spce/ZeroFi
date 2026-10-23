package com.zer0spce.zerofi.platform

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.util.ArrayDeque

/**
 * Optional accessibility automation used only after the user explicitly enables it.
 * It can turn hotspot on through Quick Settings and can also try to apply hotspot
 * name/password through the OEM Settings UI when direct tethering APIs are blocked.
 */
class HotspotAutomationService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private var pendingEnsureOn = false
    private var ensureAttempts = 0

    private var pendingProfile: HotspotProfileRequest? = null
    private var profileStage = ProfileStage.OPEN_PAGE
    private var profileAttempts = 0

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
        val packageName = event?.packageName?.toString().orEmpty()
        if (pendingEnsureOn && (packageName.isEmpty() || packageName.contains("systemui", ignoreCase = true))) {
            scheduleEnsureScan(120L)
        }
        if (pendingProfile != null && (packageName.isEmpty() || packageName.contains("settings", ignoreCase = true))) {
            scheduleProfileScan(160L)
        }
    }

    override fun onInterrupt() = Unit

    private fun requestEnsureOn() {
        if (pendingEnsureOn) return
        pendingEnsureOn = true
        ensureAttempts = 0
        performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
        scheduleEnsureScan(650L)
    }

    private fun scheduleEnsureScan(delayMs: Long) {
        handler.postDelayed({ scanQuickSettings() }, delayMs)
    }

    private fun scanQuickSettings() {
        if (!pendingEnsureOn) return
        ensureAttempts++
        val root = rootInActiveWindow
        val tile = root?.let(::findHotspotNode)

        if (tile != null) {
            when (readTileState(tile)) {
                TileState.ON -> finishEnsureOn()
                TileState.OFF -> {
                    if (clickNodeOrParent(tile)) handler.postDelayed({ finishEnsureOn() }, 700L)
                    else retryEnsureOrFinish()
                }
                TileState.UNKNOWN -> retryEnsureOrFinish()
            }
        } else retryEnsureOrFinish()
    }

    private fun retryEnsureOrFinish() {
        if (ensureAttempts < 4) scheduleEnsureScan(350L) else finishEnsureOn()
    }

    private fun finishEnsureOn() {
        if (!pendingEnsureOn) return
        pendingEnsureOn = false
        ensureAttempts = 0
        performGlobalAction(GLOBAL_ACTION_BACK)
    }

    private fun requestApplyProfile(request: HotspotProfileRequest) {
        pendingProfile = request
        profileStage = ProfileStage.OPEN_PAGE
        profileAttempts = 0
        runCatching {
            startActivity(Intent(ACTION_TETHER_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
        }.onFailure {
            runCatching {
                startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
            }
        }
        scheduleProfileScan(850L)
    }

    private fun scheduleProfileScan(delayMs: Long) {
        handler.removeCallbacks(profileScanRunnable)
        handler.postDelayed(profileScanRunnable, delayMs)
    }

    private val profileScanRunnable = Runnable { scanProfileSettings() }

    private fun scanProfileSettings() {
        val request = pendingProfile ?: return
        profileAttempts++
        val root = rootInActiveWindow
        if (root == null) {
            retryProfileOrFinish()
            return
        }

        when (profileStage) {
            ProfileStage.OPEN_PAGE -> {
                val directSsid = findEditableByLabels(root, SSID_LABELS)
                val directPassword = findEditableByLabels(root, PASSWORD_LABELS)
                if (directSsid != null || directPassword != null) {
                    var touched = false
                    directSsid?.let { touched = setNodeText(it, request.ssid) || touched }
                    if (!request.openNetwork) directPassword?.let { touched = setNodeText(it, request.password) || touched }
                    if (request.openNetwork || directPassword != null) {
                        if (touched) clickSaveLike(root)
                        finishProfileAutomation()
                        return
                    }
                }

                val hotspotPage = findClickableByLabels(root, HOTSPOT_SETTINGS_LABELS)
                if (hotspotPage != null && clickNodeOrParent(hotspotPage)) {
                    profileStage = ProfileStage.SSID
                    scheduleProfileScan(650L)
                } else {
                    profileStage = ProfileStage.SSID
                    scheduleProfileScan(300L)
                }
            }

            ProfileStage.SSID -> {
                val editable = findEditableByLabels(root, SSID_LABELS)
                if (editable != null && setNodeText(editable, request.ssid)) {
                    clickSaveLike(root)
                    profileStage = if (request.openNetwork) ProfileStage.FINALIZE else ProfileStage.PASSWORD
                    scheduleProfileScan(650L)
                    return
                }

                val row = findClickableByLabels(root, SSID_LABELS)
                if (row != null && clickNodeOrParent(row)) {
                    profileStage = ProfileStage.SSID_DIALOG
                    scheduleProfileScan(450L)
                } else retryProfileOrFinish()
            }

            ProfileStage.SSID_DIALOG -> {
                val editable = findFirstEditable(root)
                if (editable != null && setNodeText(editable, request.ssid)) {
                    clickPositiveLike(root)
                    profileStage = if (request.openNetwork) ProfileStage.FINALIZE else ProfileStage.PASSWORD
                    scheduleProfileScan(650L)
                } else retryProfileOrFinish()
            }

            ProfileStage.PASSWORD -> {
                val editable = findEditableByLabels(root, PASSWORD_LABELS)
                if (editable != null && setNodeText(editable, request.password)) {
                    clickSaveLike(root)
                    profileStage = ProfileStage.FINALIZE
                    scheduleProfileScan(650L)
                    return
                }

                val row = findClickableByLabels(root, PASSWORD_LABELS)
                if (row != null && clickNodeOrParent(row)) {
                    profileStage = ProfileStage.PASSWORD_DIALOG
                    scheduleProfileScan(450L)
                } else retryProfileOrFinish()
            }

            ProfileStage.PASSWORD_DIALOG -> {
                val editable = findFirstEditable(root)
                if (editable != null && setNodeText(editable, request.password)) {
                    clickPositiveLike(root)
                    profileStage = ProfileStage.FINALIZE
                    scheduleProfileScan(650L)
                } else retryProfileOrFinish()
            }

            ProfileStage.FINALIZE -> {
                clickSaveLike(root)
                finishProfileAutomation()
            }
        }
    }

    private fun retryProfileOrFinish() {
        if (profileAttempts < 12) scheduleProfileScan(350L) else finishProfileAutomation()
    }

    private fun finishProfileAutomation() {
        pendingProfile = null
        profileStage = ProfileStage.OPEN_PAGE
        profileAttempts = 0
        handler.removeCallbacks(profileScanRunnable)
        performGlobalAction(GLOBAL_ACTION_BACK)
    }

    private fun findEditableByLabels(root: AccessibilityNodeInfo, labels: List<String>): AccessibilityNodeInfo? {
        val nodes = flatten(root)
        return nodes.firstOrNull { node ->
            node.isEditable && labels.any { token -> surroundingLabel(node).contains(token) }
        }
    }

    private fun findFirstEditable(root: AccessibilityNodeInfo): AccessibilityNodeInfo? =
        flatten(root).firstOrNull { it.isEditable }

    private fun surroundingLabel(node: AccessibilityNodeInfo): String = buildString {
        append(buildNodeLabel(node).lowercase())
        node.parent?.let { append(' ').append(buildNodeLabel(it).lowercase()) }
    }

    private fun setNodeText(node: AccessibilityNodeInfo, value: String): Boolean {
        if (!node.isEditable) return false
        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, value)
        }
        return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    private fun clickSaveLike(root: AccessibilityNodeInfo): Boolean =
        findClickableByLabels(root, SAVE_LABELS)?.let(::clickNodeOrParent) ?: false

    private fun clickPositiveLike(root: AccessibilityNodeInfo): Boolean =
        findClickableByLabels(root, POSITIVE_LABELS)?.let(::clickNodeOrParent) ?: false

    private fun findClickableByLabels(root: AccessibilityNodeInfo, labels: List<String>): AccessibilityNodeInfo? =
        flatten(root).firstOrNull { node ->
            val label = buildNodeLabel(node).lowercase()
            labels.any(label::contains) && (node.isClickable || node.parent?.isClickable == true)
        }

    private fun flatten(root: AccessibilityNodeInfo): List<AccessibilityNodeInfo> {
        val result = ArrayList<AccessibilityNodeInfo>()
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            result.add(node)
            for (i in 0 until node.childCount) node.getChild(i)?.let(queue::addLast)
        }
        return result
    }

    private fun findHotspotNode(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            val label = buildNodeLabel(node).lowercase()
            if (HOTSPOT_LABELS.any { label.contains(it) }) return node
            for (i in 0 until node.childCount) node.getChild(i)?.let(queue::addLast)
        }
        return null
    }

    private fun buildNodeLabel(node: AccessibilityNodeInfo): String = buildString {
        node.text?.let { append(it).append(' ') }
        node.contentDescription?.let { append(it).append(' ') }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) node.hintText?.let { append(it).append(' ') }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) node.stateDescription?.let { append(it).append(' ') }
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
            if (candidate.isClickable && candidate.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
            current = candidate.parent
        }
        return false
    }

    private enum class TileState { ON, OFF, UNKNOWN }
    private enum class ProfileStage { OPEN_PAGE, SSID, SSID_DIALOG, PASSWORD, PASSWORD_DIALOG, FINALIZE }

    data class HotspotProfileRequest(
        val ssid: String,
        val password: String,
        val openNetwork: Boolean,
    )

    companion object {
        private const val ACTION_TETHER_SETTINGS = "android.settings.TETHER_SETTINGS"

        @Volatile private var instance: HotspotAutomationService? = null

        private val HOTSPOT_LABELS = listOf("mobile hotspot", "wi-fi hotspot", "wifi hotspot", "personal hotspot", "hotspot")
        private val HOTSPOT_SETTINGS_LABELS = listOf("mobile hotspot", "wi-fi hotspot", "wifi hotspot", "hotspot settings", "portable hotspot")
        private val SSID_LABELS = listOf("network name", "hotspot name", "wi-fi name", "wifi name", "ssid", "name")
        private val PASSWORD_LABELS = listOf("password", "passphrase", "hotspot password")
        private val SAVE_LABELS = listOf("save", "apply", "done")
        private val POSITIVE_LABELS = listOf("ok", "save", "apply", "done")

        fun isEnabled(context: Context): Boolean {
            val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES).orEmpty()
            val component = ComponentName(context, HotspotAutomationService::class.java).flattenToString()
            return enabled.split(':').any { it.equals(component, ignoreCase = true) }
        }

        fun requestEnsureHotspotOn() {
            val service = instance ?: return
            service.handler.post { service.requestEnsureOn() }
        }

        fun requestApplyHotspotProfile(ssid: String, password: String, openNetwork: Boolean): Boolean {
            val service = instance ?: return false
            if (ssid.isBlank()) return false
            if (!openNetwork && password.length !in 8..63) return false
            service.handler.post {
                service.requestApplyProfile(HotspotProfileRequest(ssid.trim(), password, openNetwork))
            }
            return true
        }
    }
}
