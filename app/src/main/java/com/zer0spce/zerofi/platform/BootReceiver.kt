package com.zer0spce.zerofi.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.zer0spce.zerofi.MainActivity
import com.zer0spce.zerofi.data.AppPreferences

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        if (!AppPreferences(context).autoLaunchOnBoot) return

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        runCatching { context.startActivity(launchIntent) }
    }
}
