package com.zer0spce.zerofi

import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.zer0spce.zerofi.data.AppPreferences
import com.zer0spce.zerofi.platform.HotspotAutomationService
import com.zer0spce.zerofi.platform.HotspotLaunchController
import com.zer0spce.zerofi.platform.RouterLockController
import com.zer0spce.zerofi.ui.ZeroFiApp

class MainActivity : ComponentActivity() {
    private lateinit var preferences: AppPreferences
    private val launchHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferences = AppPreferences(this)

        // ZeroFi is intended to behave like a dedicated pocket-router display.
        // Window tweaks are always fail-safe so an OEM quirk never blocks launch.
        runCatching { setKeepScreenOn(preferences.keepScreenOn) }
        runCatching { setImmersiveMode(preferences.immersiveMode) }

        setContent {
            ZeroFiApp()
        }
    }

    override fun onResume() {
        super.onResume()
        if (::preferences.isInitialized) {
            runCatching { setKeepScreenOn(preferences.keepScreenOn) }
            runCatching { setImmersiveMode(preferences.immersiveMode) }
        }
    }

    override fun onPostResume() {
        super.onPostResume()
        if (!::preferences.isInitialized) return

        launchHandler.removeCallbacksAndMessages(null)
        val hotspotAutomationEnabled = runCatching {
            HotspotAutomationService.isEnabled(this)
        }.getOrDefault(false)

        // First launch may temporarily open Accessibility settings so the user
        // can grant ZeroFi's stock-Android hotspot automation service. Do not
        // lock the user into ZeroFi until that one-time setup screen is closed.
        val openedHotspotSetup = runCatching {
            HotspotLaunchController.ensureConfiguredOrRun(this)
        }.getOrDefault(false)

        if (openedHotspotSetup) return

        runCatching { setImmersiveMode(true) }

        // When accessibility hotspot automation is available, let it open Quick
        // Settings, inspect/enable Hotspot and close the panel before true kiosk
        // mode disables access to System UI. Without automation there is nothing
        // to wait for, so Router Lock can begin immediately.
        val lockDelayMs = if (hotspotAutomationEnabled) 2_600L else 0L
        launchHandler.postDelayed({ enterRouterLockIfNeeded() }, lockDelayMs)
    }

    override fun onPause() {
        // Keep a delayed kiosk transition only while this activity is still the
        // intended foreground destination. Hotspot automation returns here and
        // onPostResume schedules the lock again if needed.
        launchHandler.removeCallbacksAndMessages(null)
        super.onPause()
    }

    override fun onDestroy() {
        launchHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    private fun enterRouterLockIfNeeded() {
        if (isFinishing || isDestroyed) return
        val alreadyLocked = runCatching {
            RouterLockController.isLockTaskRunning(this)
        }.getOrDefault(false)
        if (!alreadyLocked) {
            // Device Owner => true kiosk/lock-task with Home + Recents blocked.
            // Ordinary device => Android screen-pinning confirmation/fallback.
            runCatching { RouterLockController.start(this) }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && ::preferences.isInitialized && preferences.immersiveMode) {
            runCatching { hideSystemUi() }
        }
    }

    fun setKeepScreenOn(enabled: Boolean) {
        if (enabled) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    fun setImmersiveMode(enabled: Boolean) {
        if (enabled) hideSystemUi() else showSystemUi()
    }

    fun setAppBrightness(value: Float?) {
        val params = window.attributes
        params.screenBrightness = value?.coerceIn(0.05f, 1f)
            ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        window.attributes = params
    }

    @Suppress("DEPRECATION")
    private fun hideSystemUi() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }
    }

    @Suppress("DEPRECATION")
    private fun showSystemUi() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.show(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
        } else {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
    }
}
