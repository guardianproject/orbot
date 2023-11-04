package org.torproject.android.ui

import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.service.quicksettings.TileService

import org.torproject.android.OrbotActivity

class OrbotTileService: TileService() {

    // Called when the user adds your tile.
    override fun onTileAdded() {
        super.onTileAdded()
    }
    // Called when your app can update your tile.
    override fun onStartListening() {
        super.onStartListening()
    }

    // Called when your app can no longer update your tile.
    override fun onStopListening() {
        super.onStopListening()
    }

    // Called when the user taps on your tile in an active or inactive state.
    override fun onClick() {
        super.onClick()
        val intent = Intent(this, OrbotActivity::class.java)
            .addFlags(FLAG_ACTIVITY_NEW_TASK)

        startActivityAndCollapse(intent)
    }
    // Called when the user removes your tile.
    override fun onTileRemoved() {
        super.onTileRemoved()
    }
}