package org.torproject.android.ui.volunteer

import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import org.torproject.android.R
import org.torproject.android.service.util.Prefs

class VolunteerModeActivity: AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_volunteer)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

     //   var tvWeeklyTotal = findViewById<TextView>(R.id.tvWeeklyTotal)
        var tvAlltimeTotal = findViewById<TextView>(R.id.tvAlltimeTotal)
        var swVolunteerMode = findViewById<SwitchCompat>(R.id.swVolunteerMode)

        tvAlltimeTotal.text = Prefs.getSnowflakesServed().toString()

        swVolunteerMode.isChecked = Prefs.beSnowflakeProxy()
        swVolunteerMode.setOnCheckedChangeListener { _, isChecked ->
           Prefs.setBeSnowflakeProxy(isChecked)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}