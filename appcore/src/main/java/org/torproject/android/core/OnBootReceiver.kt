package org.torproject.android.core

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import org.torproject.android.service.OrbotConstants
import org.torproject.android.service.OrbotService
import org.torproject.android.service.util.Prefs

class OnBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Prefs.startOnBoot() && !sReceivedBoot) {
            startService(OrbotConstants.ACTION_START_ON_BOOT, context)
            sReceivedBoot = true
        }
    }

    private fun startService(action: String, context: Context) {
        val intent = Intent(context, OrbotService::class.java).apply { this.action = action }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            context.startForegroundService(intent)
        else {
            context.startService(intent)
        }
    }

    companion object {
        private var sReceivedBoot = false
    }
}