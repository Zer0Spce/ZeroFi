package com.zer0spce.zerofi.platform

import android.content.Context
import android.net.TetheringInterface
import android.net.TetheringManager
import android.net.wifi.SoftApConfiguration
import androidx.annotation.RequiresApi

@RequiresApi(36)
class Api36TetheringWatcher(private val context: Context) {
    private var callback: TetheringManager.TetheringEventCallback? = null
    private var manager: TetheringManager? = null

    fun start(onStatus: (active: Boolean?, band: String?) -> Unit) {
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

    fun stop() {
        val callbackToRemove = callback ?: return
        runCatching { manager?.unregisterTetheringEventCallback(callbackToRemove) }
        callback = null
        manager = null
    }

    private fun bandLabel(config: SoftApConfiguration): String {
        val channels = config.channels
        if (channels.size() == 0) return "System"

        val labels = buildList {
            for (index in 0 until channels.size()) {
                when (channels.keyAt(index)) {
                    SoftApConfiguration.BAND_2GHZ -> add("2.4 GHz")
                    SoftApConfiguration.BAND_5GHZ -> add("5 GHz")
                    SoftApConfiguration.BAND_6GHZ -> add("6 GHz")
                    SoftApConfiguration.BAND_60GHZ -> add("60 GHz")
                }
            }
        }.distinct()

        return when {
            labels.isEmpty() -> "System"
            labels.size == 1 -> labels.first()
            else -> labels.joinToString(" + ")
        }
    }
}
