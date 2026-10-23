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

/** Optional automation used only after the user explicitly enables Accessibility. */
class HotspotAutomationService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private var pendingEnsureTarget: TileState? = null
    private var ensureAttempts = 0

    private var pendingProfile: HotspotProfileRequest? = null
    private var profileStage = ProfileStage.OPEN_PAGE
    private var profileAttempts = 0
    private var ssidApplied = false
    private var passwordApplied = false
    private var profileResult: ((Boolean, String) -> Unit)? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        handler.removeCallbacksAndMessages(null)
        finishProfile(false, "Hotspot automation stopped before it finished")
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString().orEmpty()
        if (pendingEnsureTarget != null && (pkg.isBlank() || pkg.contains("systemui", true))) scheduleEnsureScan(120L)
        if (pendingProfile != null && (pkg.isBlank() || pkg.contains("settings", true))) scheduleProfileScan(120L)
    }

    override fun onInterrupt() = Unit

    private fun requestEnsure(target: TileState) {
        if (pendingEnsureTarget != null) return
        pendingEnsureTarget = target
        ensureAttempts = 0
        performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
        scheduleEnsureScan(600L)
    }

    private fun scheduleEnsureScan(delayMs: Long) {
        handler.postDelayed({ scanQuickSettings() }, delayMs)
    }

    private fun scanQuickSettings() {
        val target = pendingEnsureTarget ?: return
        ensureAttempts++
        val root = rootInActiveWindow
        val tile = root?.let { findBestLabelNode(it, HOTSPOT_LABELS) }
        if (tile == null) {
            retryEnsure()
            return
        }
        val state = readTileState(tile)
        when {
            state == target -> finishEnsure()
            state == TileState.UNKNOWN -> retryEnsure()
            clickNodeOrParent(tile, 8) -> handler.postDelayed({ finishEnsure() }, 800L)
            else -> retryEnsure()
        }
    }

    private fun retryEnsure() {
        if (ensureAttempts < 6) scheduleEnsureScan(320L) else finishEnsure()
    }

    private fun finishEnsure() {
        if (pendingEnsureTarget == null) return
        pendingEnsureTarget = null
        ensureAttempts = 0
        listOf(50L, 220L, 430L, 680L).forEach { delay ->
            handler.postDelayed({ backIfSystemUiVisible() }, delay)
        }
    }

    private fun backIfSystemUiVisible() {
        val pkg = rootInActiveWindow?.packageName?.toString().orEmpty()
        if (pkg.isBlank() || pkg.contains("systemui", true)) performGlobalAction(GLOBAL_ACTION_BACK)
    }

    private fun requestApplyProfile(request: HotspotProfileRequest, onResult: (Boolean, String) -> Unit) {
        if (pendingProfile != null) {
            onResult(false, "Another hotspot update is already running")
            return
        }
        pendingProfile = request
        profileResult = onResult
        profileStage = ProfileStage.OPEN_PAGE
        profileAttempts = 0
        ssidApplied = false
        passwordApplied = request.openNetwork
        launchWifiHotspotDetails()
        scheduleProfileScan(650L)
    }

    private fun launchWifiHotspotDetails() {
        val direct = Intent(ACTION_WIFI_TETHER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val explicitAosp = Intent().apply {
            component = ComponentName(SETTINGS_PACKAGE, WIFI_TETHER_ACTIVITY)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val overview = Intent(ACTION_TETHER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val wireless = Intent(Settings.ACTION_WIRELESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val launched = tryStart(direct) || tryStart(explicitAosp) || tryStart(overview) || tryStart(wireless)
        if (!launched) finishProfile(false, "Android hotspot settings could not be opened")
    }

    private fun tryStart(intent: Intent): Boolean = runCatching {
        if (intent.component != null || intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
            true
        } else false
    }.getOrDefault(false)

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
            retryProfile("Android Settings did not expose hotspot controls")
            return
        }
        when (profileStage) {
            ProfileStage.OPEN_PAGE -> {
                if (containsProfileControls(root)) {
                    profileStage = ProfileStage.SSID
                    scheduleProfileScan(100L)
                    return
                }
                val row = findActionableLabelNode(root, HOTSPOT_SETTINGS_LABELS, 12)
                if (row != null && clickNodeOrParent(row, 12)) {
                    profileStage = ProfileStage.WAIT_FOR_DETAIL
                    scheduleProfileScan(550L)
                    return
                }
                if (profileAttempts == 5 || profileAttempts == 10) launchWifiHotspotDetails()
                retryProfile("Could not enter Wi-Fi hotspot settings")
            }
            ProfileStage.WAIT_FOR_DETAIL -> {
                if (containsProfileControls(root)) {
                    profileStage = ProfileStage.SSID
                    scheduleProfileScan(100L)
                    return
                }
                val row = findActionableLabelNode(root, HOTSPOT_SETTINGS_LABELS, 12)
                if (row != null) clickNodeOrParent(row, 12)
                if (profileAttempts == 9) launchWifiHotspotDetails()
                retryProfile("Waiting for Wi-Fi hotspot settings")
            }
            ProfileStage.SSID -> {
                val editable = findEditableByLabels(root, SSID_LABELS)
                if (editable != null && setNodeText(editable, request.ssid)) {
                    ssidApplied = true
                    clickSaveLike(root)
                    profileStage = if (request.openNetwork) ProfileStage.FINALIZE else ProfileStage.PASSWORD
                    scheduleProfileScan(400L)
                    return
                }
                val row = findActionableLabelNode(root, SSID_LABELS, 10)
                if (row != null && clickNodeOrParent(row, 10)) {
                    profileStage = ProfileStage.SSID_DIALOG
                    scheduleProfileScan(320L)
                } else retryProfile("Could not find hotspot name")
            }
            ProfileStage.SSID_DIALOG -> {
                val editable = findFirstEditable(root)
                if (editable != null && setNodeText(editable, request.ssid)) {
                    ssidApplied = true
                    if (!clickPositiveLike(root)) performGlobalAction(GLOBAL_ACTION_BACK)
                    profileStage = if (request.openNetwork) ProfileStage.FINALIZE else ProfileStage.PASSWORD
                    scheduleProfileScan(420L)
                } else retryProfile("Could not edit hotspot name")
            }
            ProfileStage.PASSWORD -> {
                val editable = findEditableByLabels(root, PASSWORD_LABELS)
                if (editable != null && setNodeText(editable, request.password)) {
                    passwordApplied = true
                    clickSaveLike(root)
                    profileStage = ProfileStage.FINALIZE
                    scheduleProfileScan(400L)
                    return
                }
                val row = findActionableLabelNode(root, PASSWORD_LABELS, 10)
                if (row != null && clickNodeOrParent(row, 10)) {
                    profileStage = ProfileStage.PASSWORD_DIALOG
                    scheduleProfileScan(320L)
                } else retryProfile("Could not find hotspot password")
            }
            ProfileStage.PASSWORD_DIALOG -> {
                val editable = findFirstEditable(root)
                if (editable != null && setNodeText(editable, request.password)) {
                    passwordApplied = true
                    if (!clickPositiveLike(root)) performGlobalAction(GLOBAL_ACTION_BACK)
                    profileStage = ProfileStage.FINALIZE
                    scheduleProfileScan(420L)
                } else retryProfile("Could not edit hotspot password")
            }
            ProfileStage.FINALIZE -> {
                clickSaveLike(root)
                val success = ssidApplied && passwordApplied
                finishProfile(success, if (success) "Android hotspot updated" else "Android hotspot fields could not be fully updated")
            }
        }
    }

    private fun containsProfileControls(root: AccessibilityNodeInfo): Boolean {
        val labels = flatten(root).map { buildNodeLabel(it).lowercase() }
        return labels.any { text -> SSID_LABELS.any(text::contains) } || labels.any { text -> PASSWORD_LABELS.any(text::contains) }
    }

    private fun retryProfile(reason: String) {
        if (profileAttempts < 24) scheduleProfileScan(320L) else finishProfile(false, reason)
    }

    private fun finishProfile(success: Boolean, message: String) {
        if (pendingProfile == null && profileResult == null) return
        pendingProfile = null
        profileStage = ProfileStage.OPEN_PAGE
        profileAttempts = 0
        ssidApplied = false
        passwordApplied = false
        handler.removeCallbacks(profileScanRunnable)
        val callback = profileResult
        profileResult = null
        callback?.invoke(success, message)
        listOf(180L, 430L, 720L).forEach { delay -> handler.postDelayed({ backIfSettingsVisible() }, delay) }
    }

    private fun backIfSettingsVisible() {
        val pkg = rootInActiveWindow?.packageName?.toString().orEmpty()
        if (pkg.contains("settings", true)) performGlobalAction(GLOBAL_ACTION_BACK)
    }

    private fun findEditableByLabels(root: AccessibilityNodeInfo, labels: List<String>): AccessibilityNodeInfo? =
        flatten(root).firstOrNull { node -> node.isEditable && labels.any { surroundingLabel(node).contains(it) } }
    private fun findFirstEditable(root: AccessibilityNodeInfo): AccessibilityNodeInfo? = flatten(root).firstOrNull { it.isEditable }
    private fun surroundingLabel(node: AccessibilityNodeInfo): String = buildString {
        append(buildNodeLabel(node).lowercase())
        var parent = node.parent
        repeat(4) {
            parent ?: return@repeat
            append(' ').append(buildNodeLabel(parent).lowercase())
            parent = parent.parent
        }
    }
    private fun setNodeText(node: AccessibilityNodeInfo, value: String): Boolean {
        if (!node.isEditable) return false
        node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
        val args = Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, value) }
        return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }
    private fun clickSaveLike(root: AccessibilityNodeInfo): Boolean = findActionableLabelNode(root, SAVE_LABELS, 8)?.let { clickNodeOrParent(it, 8) } ?: false
    private fun clickPositiveLike(root: AccessibilityNodeInfo): Boolean = findActionableLabelNode(root, POSITIVE_LABELS, 8)?.let { clickNodeOrParent(it, 8) } ?: false

    private fun findBestLabelNode(root: AccessibilityNodeInfo, labels: List<String>): AccessibilityNodeInfo? {
        val nodes = flatten(root)
        labels.forEach { token -> nodes.firstOrNull { buildNodeLabel(it).trim().equals(token, true) }?.let { return it } }
        labels.forEach { token -> nodes.firstOrNull { buildNodeLabel(it).lowercase().contains(token) }?.let { return it } }
        return null
    }
    private fun findActionableLabelNode(root: AccessibilityNodeInfo, labels: List<String>, levels: Int): AccessibilityNodeInfo? {
        val nodes = flatten(root)
        labels.forEach { token -> nodes.firstOrNull { buildNodeLabel(it).trim().equals(token, true) && hasClickableNodeOrAncestor(it, levels) }?.let { return it } }
        labels.forEach { token -> nodes.firstOrNull { buildNodeLabel(it).lowercase().contains(token) && hasClickableNodeOrAncestor(it, levels) }?.let { return it } }
        return null
    }
    private fun hasClickableNodeOrAncestor(node: AccessibilityNodeInfo, levels: Int): Boolean {
        var current: AccessibilityNodeInfo? = node
        repeat(levels) {
            val candidate = current ?: return false
            if (candidate.isClickable) return true
            current = candidate.parent
        }
        return false
    }
    private fun clickNodeOrParent(node: AccessibilityNodeInfo, levels: Int): Boolean {
        var current: AccessibilityNodeInfo? = node
        repeat(levels) {
            val candidate = current ?: return false
            if (candidate.isClickable && candidate.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
            current = candidate.parent
        }
        return false
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
    private fun buildNodeLabel(node: AccessibilityNodeInfo): String = buildString {
        node.text?.let { append(it).append(' ') }
        node.contentDescription?.let { append(it).append(' ') }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) node.hintText?.let { append(it).append(' ') }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) node.stateDescription?.let { append(it).append(' ') }
    }
    private fun readTileState(node: AccessibilityNodeInfo): TileState {
        val text = buildNodeLabel(node).lowercase()
        if (node.isCheckable) return if (node.isChecked) TileState.ON else TileState.OFF
        val off = listOf(" off", "off ", "disabled", "not active", "inactive")
        val on = listOf(" on", "on ", "enabled", "active")
        if (off.any(text::contains)) return TileState.OFF
        if (on.any(text::contains)) return TileState.ON
        var parent = node.parent
        repeat(6) {
            parent ?: return@repeat
            val parentText = buildNodeLabel(parent).lowercase()
            if (parent.isCheckable) return if (parent.isChecked) TileState.ON else TileState.OFF
            if (off.any(parentText::contains)) return TileState.OFF
            if (on.any(parentText::contains)) return TileState.ON
            parent = parent.parent
        }
        return TileState.UNKNOWN
    }

    private enum class TileState { ON, OFF, UNKNOWN }
    private enum class ProfileStage { OPEN_PAGE, WAIT_FOR_DETAIL, SSID, SSID_DIALOG, PASSWORD, PASSWORD_DIALOG, FINALIZE }
    data class HotspotProfileRequest(val ssid: String, val password: String, val openNetwork: Boolean)

    companion object {
        private const val ACTION_WIFI_TETHER_SETTINGS = "com.android.settings.WIFI_TETHER_SETTINGS"
        private const val ACTION_TETHER_SETTINGS = "android.settings.TETHER_SETTINGS"
        private const val SETTINGS_PACKAGE = "com.android.settings"
        private const val WIFI_TETHER_ACTIVITY = "com.android.settings.Settings\$WifiTetherSettingsActivity"
        @Volatile private var instance: HotspotAutomationService? = null
        private val HOTSPOT_LABELS = listOf("mobile hotspot", "wi-fi hotspot", "wifi hotspot", "personal hotspot", "hotspot")
        private val HOTSPOT_SETTINGS_LABELS = listOf("wi-fi hotspot", "wifi hotspot", "mobile hotspot", "personal hotspot", "portable hotspot")
        private val SSID_LABELS = listOf("hotspot name", "network name", "wi-fi name", "wifi name", "ssid")
        private val PASSWORD_LABELS = listOf("hotspot password", "password", "passphrase")
        private val SAVE_LABELS = listOf("save", "apply", "done")
        private val POSITIVE_LABELS = listOf("ok", "save", "apply", "done")

        fun isEnabled(context: Context): Boolean {
            val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES).orEmpty()
            val component = ComponentName(context, HotspotAutomationService::class.java).flattenToString()
            return enabled.split(':').any { it.equals(component, true) }
        }
        fun requestEnsureHotspotOn(): Boolean {
            val service = instance ?: return false
            service.handler.post { service.requestEnsure(TileState.ON) }
            return true
        }
        fun requestEnsureHotspotOff(): Boolean {
            val service = instance ?: return false
            service.handler.post { service.requestEnsure(TileState.OFF) }
            return true
        }
        fun requestApplyHotspotProfile(ssid: String, password: String, openNetwork: Boolean, onResult: (Boolean, String) -> Unit): Boolean {
            val service = instance ?: return false
            if (ssid.isBlank()) return false
            if (!openNetwork && password.length !in 8..63) return false
            service.handler.post { service.requestApplyProfile(HotspotProfileRequest(ssid.trim(), password, openNetwork), onResult) }
            return true
        }
    }
}
