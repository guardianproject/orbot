package org.torproject.android

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.torproject.android.circumvention.CircumventionApiManager
import org.torproject.android.circumvention.SettingsRequest

class TestConnectionActivity : AppCompatActivity() {
    private lateinit var tvView: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.acitivty_conn_test)
        tvView = findViewById(R.id.tvView)
        val api = CircumventionApiManager()

        api.getSettings(SettingsRequest("cn"), {
            it?.let {
                Log.d("bim", "China Settings Request")
                Log.d("bim", "$it")
            }
        },{})

        api.getMap({
            Log.d("bim", "get map")
            it?.forEach { (k, v) ->
                Log.d("bim","Country $k")
                Log.d("bim", "$v")
            }
        })
    }
}