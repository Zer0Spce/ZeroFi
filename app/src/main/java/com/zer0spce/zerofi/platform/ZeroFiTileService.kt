package com.zer0spce.zerofi.platform

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.zer0spce.zerofi.MainActivity

class ZeroFiTileService : TileService() {
    override fun onStartListening() {
        super.onStartListening()
        refreshTile()
    }

    override fun onClick() {
        super.onClick()
        if (!HotspotAutomationService.isEnabled(this)) {
            openZeroFi()
            return
        }
        val active = getSharedPreferences(RouterActionReceiver.RUNTIME_PREFS, Context.MODE_PRIVATE)
            .getBoolean(RouterActionReceiver.KEY_HOTSPOT_ACTIVE, false)
        if (active) HotspotAutomationService.requestEnsureHotspotOff() else HotspotAutomationService.requestEnsureHotspotOn()
        qsTile?.state = if (active) Tile.STATE_INACTIVE else Tile.STATE_ACTIVE
        qsTile?.label = "ZeroFi"
        qsTile?.subtitle = if (active) "Turning off…" else "Turning on…"
        qsTile?.updateTile()
    }

    private fun refreshTile() {
        val active = getSharedPreferences(RouterActionReceiver.RUNTIME_PREFS, Context.MODE_PRIVATE)
            .getBoolean(RouterActionReceiver.KEY_HOTSPOT_ACTIVE, false)
        qsTile?.apply {
            state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            label = "ZeroFi"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) subtitle = if (active) "Hotspot on" else "Hotspot off"
            updateTile()
        }
    }

    private fun openZeroFi() {
        val intent = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        if (Build.VERSION.SDK_INT >= 34) {
            val pending = PendingIntent.getActivity(this, 44, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            startActivityAndCollapse(pending)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}
