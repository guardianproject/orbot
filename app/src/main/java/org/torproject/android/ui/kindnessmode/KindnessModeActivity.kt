package org.torproject.android.ui.kindnessmode

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import org.torproject.android.R
import org.torproject.android.service.OrbotConstants
import org.torproject.android.service.OrbotService
import org.torproject.android.service.util.Prefs

class KindnessModeActivity: AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_volunteer)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

     //   var tvWeeklyTotal = findViewById<TextView>(R.id.tvWeeklyTotal)
        var tvAlltimeTotal = findViewById<TextView>(R.id.tvAlltimeTotal)
        var tvWeeklyTotal = findViewById<TextView>(R.id.tvWeeklyTotal)
        var swVolunteerMode = findViewById<SwitchCompat>(R.id.swVolunteerMode)
        var btnActionActivate = findViewById<Button>(R.id.btnActionActivate)

        tvAlltimeTotal.text = Prefs.getSnowflakesServed().toString()
        tvWeeklyTotal.text = (Prefs.getSnowflakesServedWeekly()).toString()

        swVolunteerMode.isChecked = Prefs.beSnowflakeProxy()
        swVolunteerMode.setOnCheckedChangeListener { _, isChecked ->
           Prefs.setBeSnowflakeProxy(isChecked)
            showPanelStatus(isChecked)
        }


        btnActionActivate.setOnClickListener {
            //   Prefs.setBeSnowflakeProxy(true)
          //  showPanelStatus(true)
            swVolunteerMode.isChecked = true
            Prefs.setBeSnowflakeProxy(true)
            sendIntentToService(OrbotConstants.CMD_ACTIVE)
        }

        showPanelStatus(Prefs.beSnowflakeProxy())
    }

    private fun sendIntentToService(intent: Intent) = ContextCompat.startForegroundService(this, intent)
    private fun sendIntentToService(action: String) = sendIntentToService(Intent(this, OrbotService::class.java).apply {
        this.action = action
    })

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showPanelStatus(showStatus: Boolean) {

        if (showStatus) {
            findViewById<View>(R.id.panel_kindness_activate).visibility = View.GONE
            findViewById<View>(R.id.panel_kindness_status).visibility = View.VISIBLE

        }
        else
        {
            findViewById<View>(R.id.panel_kindness_activate).visibility = View.VISIBLE
            findViewById<View>(R.id.panel_kindness_status).visibility = View.GONE
        }
    }
}