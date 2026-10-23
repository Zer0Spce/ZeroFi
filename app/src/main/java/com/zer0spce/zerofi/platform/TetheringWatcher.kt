package com.zer0spce.zerofi.platform

import android.content.Context
import android.os.Build

data class TetheringStatus(
    val active: Boolean? = null,
    val band: String? = null,
    val ssid: String? = null,
    val password: String? = null,
    val security: String? = null,
)

internal interface TetheringWatcherDelegate {
    fun start(onStatus: (TetheringStatus) -> Unit)
    fun stop()
}

/**
 * API-safe tethering status facade.
 *
 * Framework API-36 types stay isolated inside Api36TetheringWatcher. The public
 * callback only contains primitives/Strings so older Android releases can load
 * this class safely.
 */
class TetheringWatcher(private val context: Context) {
    private var delegate: TetheringWatcherDelegate? = null

    fun start(onStatus: (TetheringStatus) -> Unit) {
        if (Build.VERSION.SDK_INT < 36) {
            onStatus(TetheringStatus())
            return
        }

        runCatching {
            val clazz = Class.forName(API36_WATCHER_CLASS)
            val watcher = clazz
                .getConstructor(Context::class.java)
                .newInstance(context.applicationContext) as TetheringWatcherDelegate
            delegate = watcher
            watcher.start(onStatus)
        }.onFailure {
            delegate = null
            onStatus(TetheringStatus())
        }
    }

    fun stop() {
        runCatching { delegate?.stop() }
        delegate = null
    }

    companion object {
        private const val API36_WATCHER_CLASS =
            "com.zer0spce.zerofi.platform.Api36TetheringWatcher"
    }
}
