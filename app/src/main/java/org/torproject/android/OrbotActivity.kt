package org.torproject.android

import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import org.torproject.android.core.LocaleHelper
import org.torproject.android.service.OrbotConstants
import org.torproject.android.service.OrbotService

class OrbotActivity : AppCompatActivity(), OrbotConstants {

    private lateinit var tvTitle: TextView
    private lateinit var btnStartVpn: Button

    companion object {
        const val REQUEST_CODE_VPN = 1234
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_orbot)
        tvTitle = findViewById(R.id.tvTitle)
        btnStartVpn = findViewById(R.id.btnStart)

        btnStartVpn.setOnClickListener {
            startTorAndVpn()
        }
    }

    override fun onResume() {
        super.onResume()
        sendIntentToService(OrbotConstants.CMD_ACTIVE)
    }

    private fun sendIntentToService(intent: Intent) = ContextCompat.startForegroundService(this, intent)
    private fun sendIntentToService(action: String) = sendIntentToService(Intent(this, OrbotService::class.java).apply {
        this.action = action
    })

    private fun startTorAndVpn() {
        val vpnIntent = VpnService.prepare(this)
        if (vpnIntent != null) {
            startActivityForResult(vpnIntent, REQUEST_CODE_VPN)
        } else {
            sendIntentToService(OrbotConstants.ACTION_START)
            sendIntentToService(OrbotConstants.ACTION_START_VPN)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_VPN && resultCode == RESULT_OK) {
            startTorAndVpn()
        }
    }

    override fun attachBaseContext(newBase: Context) = super.attachBaseContext(LocaleHelper.onAttach(newBase))



}