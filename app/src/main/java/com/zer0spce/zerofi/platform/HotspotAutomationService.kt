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
 * It can turn hotspot on through Quick Settings and can also apply hotspot
 * name/password through the OEM Settings UI when direct tethering APIs are blocked.
 */
class HotspotAutomationService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private var pendingEnsureOn = false
    private var ensureAttempts = 0

    private var pendingProfile: HotspotProfileRequest? = null
    private var profileStage = ProfileStage.OPEN_TETHER_PAGE
    private var profileAttempts = 0
    private var profileSsidApplied = false
    private var profilePasswordApplied = false
    private var profileResult: ((Boolean, String) -> Unit)? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        handler.removeCallbacksAndMessages(null)
        finishProfileAutomation(false, "Hotspot automation stopped before it finished")
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName?.toString().orEmpty()
        if (pendingEnsureOn && (packageName.isEmpty() || packageName.contains("systemui", ignoreCase = true))) {
            scheduleEnsureScan(120L)
        }
        if (pendingProfile != null && (packageName.isEmpty() || packageName.contains("settings", ignoreCase = true))) {
            scheduleProfileScan(140L)
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
        collapseQuickSettings()
    }

    private fun collapseQuickSettings() {
        runCatching { sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)) }
        handler.postDelayed({ backIfSystemUiVisible() }, 80L)
        handler.postDelayed({ backIfSystemUiVisible() }, 260L)
        handler.postDelayed({ backIfSystemUiVisible() }, 520L)
    }

    private fun backIfSystemUiVisible() {
        val pkg = rootInActiveWindow?.packageName?.toString().orEmpty()
        if (pkg.isEmpty() || pkg.contains("systemui", ignoreCase = true)) {
            performGlobalAction(GLOBAL_ACTION_BACK)
        }
    }

    private fun requestApplyProfile(
        request: HotspotProfileRequest,
        onResult: (Boolean, String) -> Unit,
    ) {
        if (pendingProfile != null) {
            onResult(false, "Another hotspot update is already running")
            return
        }
        pendingProfile = request
        profileResult = onResult
        profileStage = ProfileStage.OPEN_TETHER_PAGE
        profileAttempts = 0
        profileSsidApplied = false
        profilePasswordApplied = request.openNetwork

        launchHotspotSettings()
        scheduleProfileScan(700L)
    }

    private fun launchHotspotSettings() {
        val directIntent = Intent(ACTION_WIFI_TETHER_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val tetherIntent = Intent(ACTION_TETHER_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val fallback = Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val launchedDirect = runCatching {
            if (directIntent.resolveActivity(packageManager) != null) {
                startActivity(directIntent)
                true
            } else false
        }.getOrDefault(false)

        if (!launchedDirect) {
            runCatching { startActivity(tetherIntent) }
                .onFailure { runCatching { startActivity(fallback) } }
        }
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
            retryProfileOrFinish("Android Settings did not expose the hotspot controls")
            return
        }

        when (profileStage) {
            ProfileStage.OPEN_TETHER_PAGE -> {
                // If the direct Wi-Fi tether settings action worked, we may already be
                // on the hotspot detail page. Check for real profile controls first so
                // we never mistake the page title for the overview row.
                if (containsProfileControls(root)) {
                    profileStage = ProfileStage.SSID
                    scheduleProfileScan(120L)
                    return
                }

                val hotspotPage = findActionableLabelNode(root, HOTSPOT_SETTINGS_LABELS, 10)
                if (hotspotPage != null && clickNodeOrParent(hotspotPage, 10)) {
                    profileStage = ProfileStage.WAIT_FOR_HOTSPOT_PAGE
                    scheduleProfileScan(650L)
                    return
                }
                retryProfileOrFinish("Could not open Wi-Fi hotspot settings")
            }

            ProfileStage.WAIT_FOR_HOTSPOT_PAGE -> {
                if (containsProfileControls(root)) {
                    profileStage = ProfileStage.SSID
                    scheduleProfileScan(120L)
                    return
                }

                // Some Settings builds animate slowly or keep us on the overview if
                // the first click lands on the text instead of the preference row.
                // Keep retrying the actual clickable hotspot row until the detail page
                // is confirmed by the presence of SSID/password controls.
                val hotspotPage = findActionableLabelNode(root, HOTSPOT_SETTINGS_LABELS, 10)
                if (hotspotPage != null) {
                    clickNodeOrParent(hotspotPage, 10)
                    scheduleProfileScan(500L)
                    return
                }

                if (profileAttempts == 8) {
                    runCatching {
                        startActivity(Intent(ACTION_TETHER_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
                    }
                    profileStage = ProfileStage.OPEN_TETHER_PAGE
                    scheduleProfileScan(650L)
                    return
                }
                retryProfileOrFinish("Waiting for Wi-Fi hotspot settings")
            }

            ProfileStage.SSID -> {
                val editable = findEditableByLabels(root, SSID_LABELS)
                if (editable != null && setNodeText(editable, request.ssid)) {
                    profileSsidApplied = true
                    clickSaveLike(root)
                    profileStage = if (request.openNetwork) ProfileStage.FINALIZE else ProfileStage.PASSWORD
                    scheduleProfileScan(450L)
                    return
                }

                val row = findActionableLabelNode(root, SSID_LABELS, 10)
                if (row != null && clickNodeOrParent(row, 10)) {
                    profileStage = ProfileStage.SSID_DIALOG
                    scheduleProfileScan(350L)
                } else retryProfileOrFinish("Could not find the hotspot name control")
            }

            ProfileStage.SSID_DIALOG -> {
                val editable = findFirstEditable(root)
                if (editable != null && setNodeText(editable, request.ssid)) {
                    profileSsidApplied = true
                    if (!clickPositiveLike(root)) performGlobalAction(GLOBAL_ACTION_BACK)
                    profileStage = if (request.openNetwork) ProfileStage.FINALIZE else ProfileStage.PASSWORD
                    scheduleProfileScan(450L)
                } else retryProfileOrFinish("Could not edit the hotspot name")
            }

            ProfileStage.PASSWORD -> {
                val editable = findEditableByLabels(root, PASSWORD_LABELS)
                if (editable != null && setNodeText(editable, request.password)) {
                    profilePasswordApplied = true
                    clickSaveLike(root)
                    profileStage = ProfileStage.FINALIZE
                    scheduleProfileScan(450L)
                    return
                }

                val row = findActionableLabelNode(root, PASSWORD_LABELS, 10)
                if (row != null && clickNodeOrParent(row, 10)) {
                    profileStage = ProfileStage.PASSWORD_DIALOG
                    scheduleProfileScan(350L)
                } else retryProfileOrFinish("Could not find the hotspot password control")
            }

            ProfileStage.PASSWORD_DIALOG -> {
                val editable = findFirstEditable(root)
                if (editable != null && setNodeText(editable, request.password)) {
                    profilePasswordApplied = true
                    if (!clickPositiveLike(root)) performGlobalAction(GLOBAL_ACTION_BACK)
                    profileStage = ProfileStage.FINALIZE
                    scheduleProfileScan(450L)
                } else retryProfileOrFinish("Could not edit the hotspot password")
            }

            ProfileStage.FINALIZE -> {
                clickSaveLike(root)
                val success = profileSsidApplied && profilePasswordApplied
                if (success) finishProfileAutomation(true, "Android hotspot updated")
                else finishProfileAutomation(false, "Android hotspot fields could not be fully updated")
            }
        }
    }

    private fun containsProfileControls(root: AccessibilityNodeInfo): Boolean {
        val labels = flatten(root).map { buildNodeLabel(it).lowercase() }
        return labels.any { label -> SSID_LABELS.any(label::contains) } ||
            labels.any { label -> PASSWORD_LABELS.any(label::contains) }
    }

    private fun retryProfileOrFinish(reason: String) {
        if (profileAttempts < 20) scheduleProfileScan(320L)
        else finishProfileAutomation(false, reason)
    }

    private fun finishProfileAutomation(success: Boolean, message: String) {
        if (pendingProfile == null && profileResult == null) return
        pendingProfile = null
        profileStage = ProfileStage.OPEN_TETHER_PAGE
        profileAttempts = 0
        profileSsidApplied = false
        profilePasswordApplied = false
        handler.removeCallbacks(profileScanRunnable)
        val callback = profileResult
        profileResult = null
        callback?.invoke(success, message)

        // Return from Wi-Fi hotspot detail -> Hotspot & tethering -> ZeroFi.
        handler.postDelayed({ performGlobalAction(GLOBAL_ACTION_BACK) }, 180L)
        handler.postDelayed({ performGlobalAction(GLOBAL_ACTION_BACK) }, 480L)
    }

    private fun findEditableByLabels(root: AccessibilityNodeInfo, labels: List<String>): AccessibilityNodeInfo? {
        return flatten(root).firstOrNull { node ->
            node.isEditable && labels.any { token -> surroundingLabel(node).contains(token) }
        }
    }

    private fun findFirstEditable(root: AccessibilityNodeInfo): AccessibilityNodeInfo? =
        flatten(root).firstOrNull { it.isEditable }

    private fun surroundingLabel(node: AccessibilityNodeInfo): String = buildString {
        append(buildNodeLabel(node).lowercase())
        var parent = node.parent
        repeat(3) {
            parent ?: return@repeat
            append(' ').append(buildNodeLabel(parent).lowercase())
            parent = parent.parent
        }
    }

    private fun setNodeText(node: AccessibilityNodeInfo, value: String): Boolean {
        if (!node.isEditable) return false
        runCatching { node.performAction(AccessibilityNodeInfo.ACTION_FOCUS) }
        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, value)
        }
        return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    private fun clickSaveLike(root: AccessibilityNodeInfo): Boolean =
        findActionableLabelNode(root, SAVE_LABELS, 7)?.let { clickNodeOrParent(it, 7) } ?: false

    private fun clickPositiveLike(root: AccessibilityNodeInfo): Boolean =
        findActionableLabelNode(root, POSITIVE_LABELS, 7)?.let { clickNodeOrParent(it, 7) } ?: false

    private fun findBestLabelNode(root: AccessibilityNodeInfo, labels: List<String>): AccessibilityNodeInfo? {
        val nodes = flatten(root)
        labels.forEach { token ->
            nodes.firstOrNull { buildNodeLabel(it).trim().equals(token, ignoreCase = true) }?.let { return it }
        }
        labels.forEach { token ->
            nodes.firstOrNull { buildNodeLabel(it).lowercase().contains(token) }?.let { return it }
        }
        return null
    }

    private fun findActionableLabelNode(
        root: AccessibilityNodeInfo,
        labels: List<String>,
        levels: Int,
    ): AccessibilityNodeInfo? {
        val nodes = flatten(root)
        labels.forEach { token ->
            nodes.firstOrNull {
                buildNodeLabel(it).trim().equals(token, ignoreCase = true) && hasClickableNodeOrAncestor(it, levels)
            }?.let { return it }
        }
        labels.forEach { token ->
            nodes.firstOrNull {
                buildNodeLabel(it).lowercase().contains(token) && hasClickableNodeOrAncestor(it, levels)
            }?.let { return it }
        }
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

    private fun findHotspotNode(root: AccessibilityNodeInfo): AccessibilityNodeInfo? =
        findBestLabelNode(root, HOTSPOT_LABELS)

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
        repeat(5) {
            parent ?: return@repeat
            val parentLabel = buildNodeLabel(parent).lowercase()
            if (parent.isCheckable) return if (parent.isChecked) TileState.ON else TileState.OFF
            if (offTokens.any(parentLabel::contains)) return TileState.OFF
            if (onTokens.any(parentLabel::contains)) return TileState.ON
            parent = parent.parent
        }
        return TileState.UNKNOWN
    }

    private fun clickNodeOrParent(node: AccessibilityNodeInfo, levels: Int = 6): Boolean {
        var current: AccessibilityNodeInfo? = node
        repeat(levels) {
            val candidate = current ?: return false
            if (candidate.isClickable && candidate.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
            current = candidate.parent
        }
        return false
    }

    private enum class TileState { ON, OFF, UNKNOWN }
    private enum class ProfileStage {
        OPEN_TETHER_PAGE,
        WAIT_FOR_HOTSPOT_PAGE,
        SSID,
        SSID_DIALOG,
        PASSWORD,
        PASSWORD_DIALOG,
        FINALIZE,
    }

    data class HotspotProfileRequest(
        val ssid: String,
        val password: String,
        val openNetwork: Boolean,
    )

    companion object {
        private const val ACTION_TETHER_SETTINGS = "android.settings.TETHER_SETTINGS"
        private const val ACTION_WIFI_TETHER_SETTINGS = "android.settings.WIFI_TETHER_SETTINGS"

        @Volatile private var instance: HotspotAutomationService? = null

        private val HOTSPOT_LABELS = listOf(
            "mobile hotspot",
            "wi-fi hotspot",
            "wifi hotspot",
            "personal hotspot",
            "hotspot",
        )
        private val HOTSPOT_SETTINGS_LABELS = listOf(
            "wi-fi hotspot",
            "wifi hotspot",
            "mobile hotspot",
            "personal hotspot",
            "portable hotspot",
        )
        private val SSID_LABELS = listOf(
            "hotspot name",
            "network name",
            "wi-fi name",
            "wifi name",
            "ssid",
        )
        private val PASSWORD_LABELS = listOf("hotspot password", "password", "passphrase")
        private val SAVE_LABELS = listOf("save", "apply", "done")
        private val POSITIVE_LABELS = listOf("ok", "save", "apply", "done")

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

        fun requestApplyHotspotProfile(
            ssid: String,
            password: String,
            openNetwork: Boolean,
            onResult: (Boolean, String) -> Unit,
        ): Boolean {
            val service = instance ?: return false
            if (ssid.isBlank()) return false
            if (!openNetwork && password.length !in 8..63) return false
            service.handler.post {
                service.requestApplyProfile(
                    HotspotProfileRequest(ssid.trim(), password, openNetwork),
                    onResult,
                )
            }
            return true
        }
    }
}
