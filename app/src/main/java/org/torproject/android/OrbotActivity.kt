package org.torproject.android

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.SpannableString
import android.text.style.TextAppearanceSpan
import android.util.Log
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.Group
import androidx.core.content.ContextCompat
import androidx.core.view.forEach
import androidx.drawerlayout.widget.DrawerLayout
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationBarView
import com.google.android.material.navigation.NavigationView
import net.freehaven.tor.control.TorControlCommands
import org.torproject.android.circumvention.Bridges
import org.torproject.android.circumvention.CircumventionApiManager
import org.torproject.android.circumvention.SettingsRequest
import org.torproject.android.core.LocaleHelper
import org.torproject.android.core.ui.SettingsPreferencesActivity
import org.torproject.android.service.OrbotConstants
import org.torproject.android.service.OrbotService
import org.torproject.android.service.util.Prefs
import org.torproject.android.ui.AboutDialogFragment
import org.torproject.android.ui.AppManagerActivity
import org.torproject.android.ui.OrbotMenuAction
import org.torproject.android.ui.kindnessmode.KindnessModeActivity
import org.torproject.android.ui.v3onionservice.OnionServiceActivity
import org.torproject.android.ui.v3onionservice.PermissionManager
import org.torproject.android.ui.v3onionservice.clientauth.ClientAuthActivity


class OrbotActivity : AppCompatActivity(), ExitNodeDialogFragment.ExitNodeSelectedCallback, ConnectionHelperCallbacks {

    // main screen UI
    private lateinit var tvTitle: TextView
    private lateinit var tvSubtitle: TextView
    private lateinit var tvConfigure: TextView
    private lateinit var btnStartVpn: Button
    private lateinit var ivOnion: ImageView
    private lateinit var progressBar: ProgressBar
    private lateinit var lvConnectedActions: ListView
    // menu UI
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var menuToggle: ActionBarDrawerToggle
    private lateinit var navigationView: NavigationView
    private lateinit var tvPorts: TextView
    private lateinit var torStatsGroup: Group
    private lateinit var bottomNavigationView: BottomNavigationView

    private var previousReceivedTorStatus: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_orbot)
        tvTitle = findViewById(R.id.tvTitle)
        tvSubtitle = findViewById(R.id.tvSubtitle)
        tvSubtitle = findViewById(R.id.tvSubtitle)
        tvConfigure = findViewById(R.id.tvConfigure)
        btnStartVpn = findViewById(R.id.btnStart)
        ivOnion = findViewById(R.id.ivStatus)
        progressBar = findViewById(R.id.progressBar)
        lvConnectedActions = findViewById(R.id.lvConnected)

        refreshMenuList();

        drawerLayout = findViewById(R.id.   drawerLayout)
        navigationView = findViewById(R.id.navigationView)
        configureNavigationMenu()
        menuToggle = ActionBarDrawerToggle(this, drawerLayout, R.string.drawer_open, R.string.drawer_close)
        drawerLayout.addDrawerListener(menuToggle)
        menuToggle.syncState()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        bottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNavigationView.setOnItemSelectedListener(NavigationBarView.OnItemSelectedListener { item ->
            when(item.itemId) {
                R.id.menu_connect -> {
                    // Respond to navigation item 1 click
                    true
                }
                R.id.menu_kindness -> {
                    // Respond to navigation item 2 click
                    openKindnessMode()
                    true
                }
                R.id.menu_more -> {
                    // Respond to navigation item 2 click
                    openDrawer()
                    true
                }
                else -> false
            }
        })
        
        doLayoutOff()

        with(LocalBroadcastManager.getInstance(this)) {
            registerReceiver(orbotServiceBroadcastReceiver, IntentFilter(OrbotConstants.LOCAL_ACTION_STATUS))
            registerReceiver(orbotServiceBroadcastReceiver, IntentFilter(OrbotConstants.LOCAL_ACTION_LOG))
            registerReceiver(orbotServiceBroadcastReceiver, IntentFilter(OrbotConstants.LOCAL_ACTION_PORTS))
            registerReceiver(orbotServiceBroadcastReceiver, IntentFilter(OrbotConstants.LOCAL_ACTION_SMART_CONNECT_EVENT))
        }
    }

    private fun openExitNodeDialog() {
        ExitNodeDialogFragment(this).show(supportFragmentManager, "ExitNodeDialogFragment")
    }

    private fun configureNavigationMenu() {
        navigationView.getHeaderView(0).let {
            tvPorts = it.findViewById(R.id.tvPorts)
            torStatsGroup = it.findViewById(R.id.torStatsGroup)
        }
        // apply theme to colorize menu headers
        navigationView.menu.forEach { menu -> menu.subMenu?.let { // if it has a submenu, we want to color it
            menu.title = SpannableString(menu.title).apply {
                setSpan(TextAppearanceSpan(this@OrbotActivity, R.style.NavigationGroupMenuHeaders), 0, this.length, 0)
            }
        } }
        // set click listeners for menu items
        navigationView.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.menu_tor_connection -> {
                    openConfigureTorConnection()
                    closeDrawer()
                }
                R.id.menu_help_others -> openKindnessMode()
                R.id.menu_exit -> doExit()
                R.id.menu_v3_onion_services -> startActivity(Intent(this, OnionServiceActivity::class.java))
                R.id.menu_v3_onion_client_auth -> startActivity(Intent(this, ClientAuthActivity::class.java))
                R.id.menu_settings -> startActivityForResult(SettingsPreferencesActivity.createIntent(this, R.xml.preferences), REQUEST_CODE_SETTINGS)
                R.id.menu_faq -> Toast.makeText(this, "TODO FAQ not implemented...", Toast.LENGTH_LONG).show()
                R.id.menu_about -> {
                    AboutDialogFragment()
                        .show(supportFragmentManager, AboutDialogFragment.TAG)
                    closeDrawer()
                }
                else -> {}
            }
            true
        }

    }

    private fun openDrawer() = drawerLayout.openDrawer(Gravity.LEFT)

    private fun closeDrawer() {
        drawerLayout.closeDrawer(Gravity.LEFT)
        bottomNavigationView.selectedItemId = R.id.menu_connect
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        menuToggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item)

    override fun onResume() {
        super.onResume()
        sendIntentToService(OrbotConstants.CMD_ACTIVE)
        bottomNavigationView.selectedItemId = R.id.menu_connect

        if (Prefs.isPowerUserMode())
        {
            btnStartVpn.text = getString(R.string.connect)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(orbotServiceBroadcastReceiver)
    }

    private fun sendIntentToService(intent: Intent) = ContextCompat.startForegroundService(this, intent)
    private fun sendIntentToService(action: String) = sendIntentToService(Intent(this, OrbotService::class.java).apply {
        this.action = action
    })

    private fun startTorAndVpnDelay(ms: Long) = Handler(Looper.getMainLooper()).postDelayed({startTorAndVpn()}, ms)


    private fun startTorAndVpn() {
        val vpnIntent = VpnService.prepare(this)
        if (vpnIntent != null && (!Prefs.isPowerUserMode())) {
            startActivityForResult(vpnIntent, REQUEST_CODE_VPN)
        } else {
            // todo we need to add a power user mode for users to start the VPN without tor
            Prefs.putUseVpn(!Prefs.isPowerUserMode())
            sendIntentToService(OrbotConstants.ACTION_START)

            if (!Prefs.isPowerUserMode())
                sendIntentToService(OrbotConstants.ACTION_START_VPN)
        }
    }

    private fun stopTorAndVpn() {
        sendIntentToService(OrbotConstants.ACTION_STOP)
        sendIntentToService(OrbotConstants.ACTION_STOP_VPN)
    }

    private fun sendNewnymSignal() {
        sendIntentToService(TorControlCommands.SIGNAL_NEWNYM)
        ivOnion.animate().alpha(0f).duration = 500
        Handler().postDelayed({ ivOnion.animate().alpha(1f).duration = 500 }, 600)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_VPN && resultCode == RESULT_OK) {
            startTorAndVpn()
        } else if (requestCode == REQUEST_CODE_SETTINGS && resultCode == RESULT_OK) {
            // todo respond to language change extra data here...
        } else if (requestCode == REQUEST_VPN_APP_SELECT && resultCode == RESULT_OK) {
            sendIntentToService(OrbotConstants.ACTION_RESTART_VPN) // is this enough todo?
            refreshMenuList()
        }
    }

    private fun refreshMenuList () {

        val listItems = arrayListOf(OrbotMenuAction(R.string.btn_change_exit, 0) {openExitNodeDialog()},
            OrbotMenuAction(R.string.btn_refresh, R.drawable.ic_refresh) {sendNewnymSignal()},
            OrbotMenuAction(R.string.btn_tor_off, R.drawable.ic_power) {stopTorAndVpn()})
        if (CAN_DO_APP_ROUTING && (!Prefs.isPowerUserMode())) listItems.add(0, OrbotMenuAction(R.string.btn_choose_apps, R.drawable.ic_choose_apps) {
            startActivityForResult(Intent(this, AppManagerActivity::class.java), REQUEST_VPN_APP_SELECT)
        })
        lvConnectedActions.adapter = OrbotMenuActionAdapter(this, listItems)
    }

    override fun attachBaseContext(newBase: Context) = super.attachBaseContext(LocaleHelper.onAttach(newBase))

    private var circumventionApiBridges: List<Bridges?>? = null
    private var circumventionApiIndex = 0

    private val orbotServiceBroadcastReceiver = object : BroadcastReceiver(){
        @SuppressLint("SetTextI18n")
        override fun onReceive(context: Context?, intent: Intent?) {
            val status = intent?.getStringExtra(OrbotConstants.EXTRA_STATUS)
            when (intent?.action) {
                OrbotConstants.LOCAL_ACTION_STATUS -> {
                   if (status.equals(previousReceivedTorStatus)) return
                    when (status) {
                        OrbotConstants.STATUS_OFF -> {
                            if (previousReceivedTorStatus.equals(OrbotConstants.STATUS_STARTING)) {
                                if (allCircumventionAttemtsFailed) {
                                    allCircumventionAttemtsFailed = false
                                    return
                                }
                                var shouldDoOffLayout = true
                                if (Prefs.getConnectionPathway().equals(Prefs.PATHWAY_SMART)) {
                                    shouldDoOffLayout = false
                                }
                                if (shouldDoOffLayout) doLayoutOff()
                            }
                            else
                                doLayoutOff()
                        }
                        OrbotConstants.STATUS_STARTING -> doLayoutStarting()
                        OrbotConstants.STATUS_ON -> doLayoutOn()
                        OrbotConstants.STATUS_STOPPING -> {}
                    }

                    previousReceivedTorStatus = status

                }
                OrbotConstants.LOCAL_ACTION_LOG -> {
                    intent.getStringExtra(OrbotConstants.LOCAL_EXTRA_BOOTSTRAP_PERCENT)?.let {
                        progressBar.progress = Integer.parseInt(it)
                    }
                }
                OrbotConstants.LOCAL_ACTION_PORTS -> {
                    val socks = intent.getIntExtra(OrbotConstants.EXTRA_SOCKS_PROXY_PORT, -1)
                    val http = intent.getIntExtra(OrbotConstants.EXTRA_HTTP_PROXY_PORT, -1)
                    if (http > 0 && socks > 0) tvPorts.text = "SOCKS $socks | HTTP $http"
                }
                OrbotConstants.LOCAL_ACTION_SMART_CONNECT_EVENT -> {
                    val status = intent.getStringExtra(OrbotConstants.LOCAL_EXTRA_SMART_STATUS)
                    if (status.equals(OrbotConstants.SMART_STATUS_NO_DIRECT)) {
                        // try the circumvention API, perhaps there's something we can do
                        // TODO hardcoded on China here
//                        CircumventionApiManager().getSettings(SettingsRequest(), {
                        CircumventionApiManager().getSettings(SettingsRequest("cn"), {
                            it?.let {
                                circumventionApiBridges = it.settings
                                if (circumventionApiBridges == null) {
                                    Log.d("bim", "settings is null, we can assume a direct connect is fine ")
                                } else {
                                    Log.d("bim", "settings is $circumventionApiBridges")
                                    circumventionApiBridges?.forEach { b->
                                        Log.d("bim", "BRIDGE $b")

                                    }
                                    setPreferenceForSmartConnect()
                                }
                            }
                        }, {
                            // TODO what happens to the app in this case?!
                            Log.e("bim", "Coudln't hit circumvention API... $it")
                        })
                    } else if (status.equals(OrbotConstants.SMART_STATUS_CIRCUMVENTION_ATTEMPT_FAILED)) {
                        // our attempt at circumventing failed, let's try another if any
                        setPreferenceForSmartConnect()
                    }
                }
                else -> {}
            }
        }
    }

    private fun doLayoutForCircumventionApi() {
        Log.d("bim", "doLayoutForCircumventionApi")
        // TODO prompt user to request bridge over MOAT
        progressBar.progress = 0
        tvTitle.text = getString(R.string.having_trouble)
        tvSubtitle.text = getString(R.string.having_trouble_subtitle)
        tvSubtitle.visibility = View.VISIBLE
        btnStartVpn.text = getString(R.string.solve_captcha)
        btnStartVpn.setOnClickListener {
            MoatBottomSheet(this).show(supportFragmentManager, "CircumventionFailed")
        }
        tvConfigure.text = getString(android.R.string.cancel)
        tvConfigure.setOnClickListener {
            doLayoutOff()
        }
    }

    var allCircumventionAttemtsFailed = false
    private fun setPreferenceForSmartConnect() {
        Log.d("bim", "setPreferenceForSmartConnect()")
        val MS_DELAY = 250L
        circumventionApiBridges?.let {
            if (it.size == circumventionApiIndex) {
                Log.d("bim", "tried all attempts, got nowhere!!!")
                circumventionApiBridges = null
                circumventionApiIndex = 0
                allCircumventionAttemtsFailed = true
                doLayoutForCircumventionApi()
                return
            }
            val b = it[circumventionApiIndex]!!.bridges
            if (b.type == CircumventionApiManager.BRIDGE_TYPE_SNOWFLAKE) {
                Log.d("bim", "trying snowflake")
                Prefs.putPrefSmartTrySnowflake(true)
                startTorAndVpnDelay(MS_DELAY)
            } else if (b.type == CircumventionApiManager.BRIDGE_TYPE_OBFS4) {
                Log.d("bim", "trying obfs4 ${b.source}")
                var bridgeStrings = ""
                b.bridge_strings!!.forEach { bridgeString ->
                    bridgeStrings += "$bridgeString\n"
                }
                Prefs.putPrefSmartTryObfs4(bridgeStrings)
                startTorAndVpnDelay(MS_DELAY)
            }
            circumventionApiIndex += 1
        }
    }



    private fun doLayoutOff() {
        ivOnion.setImageResource(R.drawable.orbioff)
        (ivOnion.layoutParams as ViewGroup.MarginLayoutParams).topMargin = 250
        ivOnion.animate().y(200f).duration = 150


        tvSubtitle.visibility = View.VISIBLE
        progressBar.visibility = View.INVISIBLE
        lvConnectedActions.visibility = View.GONE
        tvTitle.text = getString(R.string.secure_your_connection_title)
        tvSubtitle.text = getString(R.string.secure_your_connection_subtitle)
        tvConfigure.visibility = View.VISIBLE
        tvConfigure.text = getString(R.string.btn_configure)
        tvConfigure.setOnClickListener {openConfigureTorConnection()}
        torStatsGroup.visibility = View.GONE
        tvPorts.text = getString(R.string.ports_not_set)
        with(btnStartVpn) {
            visibility = View.VISIBLE

            if (Prefs.isPowerUserMode())
                text = getString(R.string.connect)
            else
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
        ivOnion.setImageResource(R.drawable.orbion)

        ivOnion.animation?.cancel()
        ivOnion.animate().y(14f).duration = 200

        tvSubtitle.visibility = View.GONE
        progressBar.visibility = View.INVISIBLE
        tvTitle.text = getString(R.string.connected_title)
        tvSubtitle.visibility = View.GONE
        btnStartVpn.visibility = View.GONE
        lvConnectedActions.visibility = View.VISIBLE
        tvConfigure.visibility = View.GONE
    }

    private fun doLayoutStarting() {
        torStatsGroup.visibility = View.VISIBLE
        tvSubtitle.visibility = View.GONE
        with(progressBar) {
            progress = 0
            visibility = View.VISIBLE
        }
        ivOnion.setImageResource(R.drawable.orbistarting)
        var animHover = AnimationUtils.loadAnimation(this, R.anim.hover);
        animHover.repeatCount = Animation.INFINITE
        animHover.repeatMode = Animation.REVERSE
        ivOnion.animation = animHover
        animHover.start()

        tvTitle.text = getString(R.string.trying_to_connect_title)
        with(btnStartVpn) {
            text = getString(android.R.string.cancel)
            isEnabled = true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(
                        this@OrbotActivity,
                        R.color.orbot_btn_enabled_purple
                    )
                )
            }
            setOnClickListener { stopTorAndVpn() }
        }
    }

    // todo not really defined what this does, somehow start/manage being a snowflake proxy
    private fun openKindnessMode() {    // todo not really defined yet

        //Toast.makeText(this, "Volunteer Mode Not Implemented...", Toast.LENGTH_LONG).show()
        startActivity(Intent(this, KindnessModeActivity::class.java))

        progressBar.visibility = View.GONE    // todo not really defined yet

        tvTitle.text = getString(R.string.connected_title)
    }

    private fun openConfigureTorConnection() =
//        startActivity(Intent(this, TestConnectionActivity::class.java)) // TODO debugging
        ConfigConnectionBottomSheet(this).show(supportFragmentManager, OrbotActivity::class.java.simpleName)



    companion object {
        const val REQUEST_CODE_VPN = 1234
        const val REQUEST_CODE_SETTINGS = 2345
        const val REQUEST_VPN_APP_SELECT = 2432
        private val CAN_DO_APP_ROUTING = PermissionManager.isLollipopOrHigher()
    }

    override fun onExitNodeSelected(exitNode: String, countryDisplayName: String) {

        //tor format expects "{" for country code
        Prefs.setExitNodes("{$exitNode}");

        sendIntentToService(
            Intent(this, OrbotService::class.java)
                .setAction(OrbotConstants.CMD_SET_EXIT)
                .putExtra("exit", exitNode)
        )

        refreshMenuList()

    }

    override fun tryConnecting() {
        startTorAndVpn() // TODO for now just start tor and VPN, we need to decouple this down the line
    }

    private fun doExit() {
        val killIntent = Intent(this, OrbotService::class.java)
            .setAction(OrbotConstants.ACTION_STOP)
            .putExtra(OrbotConstants.ACTION_STOP_FOREGROUND_TASK, true)
        sendIntentToService(OrbotConstants.ACTION_STOP_VPN)
        startService(killIntent)
        finish()
    }

}