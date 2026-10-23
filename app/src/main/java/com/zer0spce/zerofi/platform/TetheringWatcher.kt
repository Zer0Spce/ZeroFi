package com.zer0spce.zerofi.platform

import android.content.Context
import android.os.Build

internal interface TetheringWatcherDelegate {
    fun start(onStatus: (active: Boolean?, band: String?) -> Unit)
    fun stop()
}

/**
 * API-safe tethering status facade.
 *
 * Important: this class contains no direct reference to API-36 framework types
 * or to Api36TetheringWatcher in its field/method signatures. Older Android
 * releases can therefore load it without resolving newer tethering classes.
 */
class TetheringWatcher(private val context: Context) {
    private var delegate: TetheringWatcherDelegate? = null

    fun start(onStatus: (active: Boolean?, band: String?) -> Unit) {
        if (Build.VERSION.SDK_INT < 36) {
            onStatus(null, null)
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
            onStatus(null, null)
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
