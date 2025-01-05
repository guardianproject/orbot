package org.torproject.android.ui

import android.app.PendingIntent
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Build
import android.service.quicksettings.TileService

import androidx.annotation.RequiresApi

import org.torproject.android.OrbotActivity

@RequiresApi(Build.VERSION_CODES.N)
class OrbotTileService : TileService() {
    override fun onClick() {
        super.onClick()
        val intent = Intent(this, OrbotActivity::class.java).apply {
            addFlags(FLAG_ACTIVITY_NEW_TASK)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
            startActivityAndCollapse(pendingIntent)
        } else {
            startActivityAndCollapse(intent)
        }
    }
}