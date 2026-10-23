package com.zer0spce.zerofi.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class RouterActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != RouterStatusNotifier.ACTION_TOGGLE_HOTSPOT) return
        val active = context.getSharedPreferences(RUNTIME_PREFS, Context.MODE_PRIVATE).getBoolean(KEY_HOTSPOT_ACTIVE, false)
        if (active) HotspotAutomationService.requestEnsureHotspotOff() else HotspotAutomationService.requestEnsureHotspotOn()
    }

    companion object {
        const val RUNTIME_PREFS = "zerofi_runtime"
        const val KEY_HOTSPOT_ACTIVE = "hotspot_active"
    }
}
