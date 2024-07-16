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
        try {
            if (Prefs.startOnBoot() && !sReceivedBoot) {
                //   if (isNetworkAvailable(context)) {
                startService(OrbotConstants.ACTION_START, context)
                sReceivedBoot = true
                // }
            }
        }
        catch (re: java.lang.RuntimeException) {
            //catch this to avoid malicious launches as document Cure53 Audit: ORB-01-009 WP1/2: Orbot DoS via exported activity (High)
        }
    }

    private fun startService(action: String, context: Context) {
        try {
            val intent = Intent(context, OrbotService::class.java).apply {
                this.action = action
            }.putNotSystem()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                context.startForegroundService(intent)
            else {
                context.startService(intent)
            }
        }
        catch (re: java.lang.RuntimeException) {
            //catch this to avoid malicious launches as document Cure53 Audit: ORB-01-009 WP1/2: Orbot DoS via exported activity (High)
        }
    }

    companion object {
        private var sReceivedBoot = false
    }
}