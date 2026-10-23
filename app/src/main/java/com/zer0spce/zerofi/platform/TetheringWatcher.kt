package com.zer0spce.zerofi.platform

import android.content.Context
import android.net.TetheringInterface
import android.net.TetheringManager
import android.net.wifi.SoftApConfiguration
import android.os.Build
import androidx.annotation.RequiresApi

class TetheringWatcher(private val context: Context) {
    private var callback: TetheringManager.TetheringEventCallback? = null
    private var manager: TetheringManager? = null

    fun start(onStatus: (active: Boolean?, band: String?) -> Unit) {
        if (Build.VERSION.SDK_INT < 36) {
            onStatus(null, null)
            return
        }
        startApi36(onStatus)
    }

    fun stop() {
        if (Build.VERSION.SDK_INT < 36) return
        stopApi36()
    }

    @RequiresApi(36)
    private fun startApi36(onStatus: (Boolean?, String?) -> Unit) {
        val tetheringManager = context.getSystemService(TetheringManager::class.java)
        val eventCallback = object : TetheringManager.TetheringEventCallback {
            override fun onTetheredInterfacesChanged(interfaces: MutableSet<TetheringInterface>) {
                val wifi = interfaces.firstOrNull { it.type == TetheringManager.TETHERING_WIFI }
                onStatus(wifi != null, wifi?.softApConfiguration?.let(::bandLabel))
            }
        }

        manager = tetheringManager
        callback = eventCallback
        runCatching {
            tetheringManager.registerTetheringEventCallback(context.mainExecutor, eventCallback)
        }.onFailure {
            manager = null
            callback = null
            onStatus(null, null)
        }
    }

    @RequiresApi(36)
    private fun stopApi36() {
        val callbackToRemove = callback ?: return
        runCatching { manager?.unregisterTetheringEventCallback(callbackToRemove) }
        callback = null
        manager = null
    }

    @RequiresApi(36)
    private fun bandLabel(config: SoftApConfiguration): String = when (config.band) {
        SoftApConfiguration.BAND_2GHZ -> "2.4 GHz"
        SoftApConfiguration.BAND_5GHZ -> "5 GHz"
        SoftApConfiguration.BAND_6GHZ -> "6 GHz"
        SoftApConfiguration.BAND_60GHZ -> "60 GHz"
        else -> "Multi-band"
    }
}
