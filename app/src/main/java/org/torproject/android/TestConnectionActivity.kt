package org.torproject.android

import IPtProxy.IPtProxy
import IPtProxy.IPtProxy.*
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import org.torproject.android.service.OrbotService
import org.torproject.android.ui.onboarding.ProxiedHurlStack
import java.io.File

class TestConnectionActivity : AppCompatActivity() {
    private val moatBaseUrl = "https://bridges.torproject.org/moat"

    private lateinit var tvView: TextView
    private lateinit var queue: RequestQueue
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.acitivty_conn_test)
        tvView = findViewById(R.id.tvView)


        val api = CircumventionApiManager()
        api.getCountries {
            if (it == null) Toast.makeText(this, "Some Error", Toast.LENGTH_LONG).show()
            else {
                for (s in it) {
                   Toast.makeText(this, s, Toast.LENGTH_LONG).show()
                }
            }
        }

    }


}