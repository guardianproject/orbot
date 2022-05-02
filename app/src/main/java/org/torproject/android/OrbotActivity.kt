package org.torproject.android

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import net.freehaven.tor.control.TorControlCommands
import org.torproject.android.core.LocaleHelper
import org.torproject.android.service.OrbotConstants
import org.torproject.android.service.OrbotService
import org.torproject.android.ui.OrbotMenuAction

class OrbotActivity : AppCompatActivity() {

    private lateinit var tvTitle: TextView
    private lateinit var tvSubtitle: TextView
    private lateinit var tvConfigure: TextView
    private lateinit var btnStartVpn: Button
    private lateinit var ivOnion: ImageView
    private lateinit var progressBar: ProgressBar
    private lateinit var lvConnectedActions: ListView

    private var previousReceivedTorStatus: String? = null

    companion object {
        const val REQUEST_CODE_VPN = 1234
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_orbot)
        tvTitle = findViewById(R.id.tvTitle)
        tvSubtitle = findViewById(R.id.tvSubtitle)
        tvConfigure = findViewById(R.id.tvConfigure)
        btnStartVpn = findViewById(R.id.btnStart)
        ivOnion = findViewById(R.id.ivStatus)
        progressBar = findViewById(R.id.progressBar)
        lvConnectedActions = findViewById(R.id.lvConnected)
        lvConnectedActions.adapter = OrbotMenuActionAdapter(this, arrayListOf(
            OrbotMenuAction(R.string.btn_choose_apps, R.drawable.ic_choose_apps) {},
            OrbotMenuAction(R.string.btn_change_exit, R.drawable.ic_choose_apps) {},
            OrbotMenuAction(R.string.btn_refresh, R.drawable.ic_refresh) {sendNewnymSignal()},
            OrbotMenuAction(R.string.btn_tor_off, R.drawable.ic_power) {stopTorAndVpn()}
        ))



        doLayoutOff()

        with(LocalBroadcastManager.getInstance(this)) {
            registerReceiver(orbotServiceBroadcastReceiver, IntentFilter(OrbotConstants.LOCAL_ACTION_STATUS))
            registerReceiver(orbotServiceBroadcastReceiver, IntentFilter(OrbotConstants.LOCAL_ACTION_LOG))
        }

    }

    override fun onResume() {
        super.onResume()
        sendIntentToService(OrbotConstants.CMD_ACTIVE)
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(orbotServiceBroadcastReceiver)
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

    private fun stopTorAndVpn() {
        sendIntentToService(OrbotConstants.ACTION_STOP)
        sendIntentToService(OrbotConstants.ACTION_STOP_VPN)
    }

    private fun sendNewnymSignal() {
        sendIntentToService(TorControlCommands.SIGNAL_NEWNYM)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_VPN && resultCode == RESULT_OK) {
            startTorAndVpn()
        }
    }

    override fun attachBaseContext(newBase: Context) = super.attachBaseContext(LocaleHelper.onAttach(newBase))

    private val orbotServiceBroadcastReceiver = object : BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            val status = intent?.getStringExtra(OrbotConstants.EXTRA_STATUS)
            when (intent?.action) {
                OrbotConstants.LOCAL_ACTION_STATUS -> {
                    if (status.equals(previousReceivedTorStatus)) return
                    previousReceivedTorStatus = status
                    when (status) {
                        OrbotConstants.STATUS_OFF -> doLayoutOff()
                        OrbotConstants.STATUS_STARTING -> doLayoutStarting()
                        OrbotConstants.STATUS_ON -> doLayoutOn()
                        OrbotConstants.STATUS_STOPPING -> {}
                    }
                }
                OrbotConstants.LOCAL_ACTION_LOG -> {
                    intent.getStringExtra(OrbotConstants.LOCAL_EXTRA_BOOTSTRAP_PERCENT)?.let {
                        progressBar.progress = Integer.parseInt(it)
                    }
                }
                else -> {}
            }
        }
    }

    private fun doLayoutOff() {
        ivOnion.setImageResource(R.drawable.ic_disconnected)
        tvSubtitle.visibility = View.VISIBLE
        progressBar.visibility = View.INVISIBLE
        lvConnectedActions.visibility = View.GONE
        tvTitle.text = getString(R.string.secure_your_connection_title)
        tvConfigure.visibility = View.VISIBLE
        with(btnStartVpn) {
            visibility = View.VISIBLE
            text = getString(R.string.btn_start_vpn)
            isEnabled = true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(
                        this@OrbotActivity,
                        R.color.orbot_btn_enabled_purple
                    )
                )
            }
            setOnClickListener { startTorAndVpn() }
        }
    }

    private fun doLayoutOn() {
        ivOnion.setImageResource(R.drawable.ic_connected)
        tvSubtitle.visibility = View.GONE
        progressBar.visibility = View.INVISIBLE
        tvTitle.text = getString(R.string.connected_title)
        tvSubtitle.visibility = View.GONE
        btnStartVpn.visibility = View.GONE
        lvConnectedActions.visibility = View.VISIBLE
        tvConfigure.visibility = View.GONE
    }

    private fun doLayoutStarting() {
        tvSubtitle.visibility = View.VISIBLE
        with(progressBar) {
            progress = 0
            visibility = View.VISIBLE
        }
        tvTitle.text = getString(R.string.trying_to_connect_title)
        with(btnStartVpn) {
            text = getString(android.R.string.cancel)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(
                        this@OrbotActivity,
                        R.color.orbot_btn_disable_grey
                    )
                )
            }
            setOnClickListener { stopTorAndVpn() }
        }
    }
}