package org.torproject.android.ui

import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Build
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import org.torproject.android.OrbotActivity

class OrbotTileService: TileService() {

    // Called when the user taps on your tile in an active or inactive state.
    override fun onClick() {
        super.onClick()
        val intent = Intent(this, OrbotActivity::class.java)
            .addFlags(FLAG_ACTIVITY_NEW_TASK)

        startActivityAndCollapse(intent)
    }
}