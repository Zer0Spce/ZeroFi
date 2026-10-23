package com.zer0spce.zerofi.platform

import android.content.Context
import android.os.Build

/**
 * API-safe tethering status facade. This class intentionally contains no direct
 * references to newer tethering framework types so it can load on Android 6.
 */
class TetheringWatcher(private val context: Context) {
    private var api36Watcher: Api36TetheringWatcher? = null

    fun start(onStatus: (active: Boolean?, band: String?) -> Unit) {
        if (Build.VERSION.SDK_INT < 36) {
            onStatus(null, null)
            return
        }

        val watcher = Api36TetheringWatcher(context.applicationContext)
        api36Watcher = watcher
        watcher.start(onStatus)
    }

    fun stop() {
        if (Build.VERSION.SDK_INT >= 36) {
            api36Watcher?.stop()
        }
        api36Watcher = null
    }
}
