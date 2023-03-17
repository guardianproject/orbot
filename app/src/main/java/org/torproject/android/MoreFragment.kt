package org.torproject.android

import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import org.torproject.android.OrbotActivity.Companion.REQUEST_CODE_SETTINGS
import org.torproject.android.OrbotActivity.Companion.REQUEST_VPN_APP_SELECT
import org.torproject.android.core.ui.SettingsPreferencesActivity
import org.torproject.android.service.OrbotConstants
import org.torproject.android.service.OrbotService
import org.torproject.android.ui.*
import org.torproject.android.ui.v3onionservice.OnionServiceActivity
import org.torproject.android.ui.v3onionservice.clientauth.ClientAuthActivity

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [MoreFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class MoreFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private lateinit var lvMore : ListView;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        var view = inflater.inflate(R.layout.fragment_more, container, false)

        var tvVersion = view.findViewById<TextView>(R.id.tvVersion)
        tvVersion.text = "Tor v" + getTorVersion()


        lvMore = view.findViewById(R.id.lvMoreActions)

        val listItems = arrayListOf(
            OrbotMenuAction(R.string.v3_hosted_services, R.drawable.ic_menu_onion) { startActivity(Intent(requireActivity(), OnionServiceActivity::class.java))},
            OrbotMenuAction(R.string.v3_client_auth_activity_title, R.drawable.ic_shield) { startActivity(Intent(requireActivity(), ClientAuthActivity::class.java))},
            OrbotMenuAction(R.string.btn_choose_apps, R.drawable.ic_choose_apps) {
                activity?.startActivityForResult(Intent(requireActivity(), AppManagerActivity::class.java), REQUEST_VPN_APP_SELECT)
            },
            OrbotMenuAction(R.string.menu_settings, R.drawable.ic_settings_gear) {
                activity?.startActivityForResult(SettingsPreferencesActivity.createIntent(requireActivity(), R.xml.preferences), REQUEST_CODE_SETTINGS)
                                                                                 },
            OrbotMenuAction(R.string.menu_log, R.drawable.ic_log) { showLog()},
            OrbotMenuAction(R.string.menu_about, R.drawable.ic_about) { AboutDialogFragment()
                .show(requireActivity().supportFragmentManager, AboutDialogFragment.TAG)},
            OrbotMenuAction(R.string.menu_exit, R.drawable.ic_exit) { doExit()}
        )
        lvMore.adapter = MoreActionAdapter(requireActivity(), listItems)

        return view;
    }

    private fun getTorVersion(): String? {
        return OrbotService.BINARY_TOR_VERSION.split("-").toTypedArray()[0]
    }


    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment MoreFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            MoreFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    private fun doExit() {
        val killIntent = Intent(requireActivity(), OrbotService::class.java)
            .setAction(OrbotConstants.ACTION_STOP)
            .putExtra(OrbotConstants.ACTION_STOP_FOREGROUND_TASK, true)
        sendIntentToService(OrbotConstants.ACTION_STOP_VPN)
        requireActivity()?.startService(killIntent)
        requireActivity()?.finish()
    }

    private fun sendIntentToService(intent: Intent) = ContextCompat.startForegroundService(requireActivity(), intent)
    private fun sendIntentToService(action: String) = sendIntentToService(
        android.content.Intent(
            requireActivity(),
            org.torproject.android.service.OrbotService::class.java
        ).apply {
            this.action = action
        })


    private fun showLog() {
        (activity as OrbotActivity).showLog()
    }

    /**
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
    //closeDrawer()
    }
    R.id.menu_help_others -> openKindnessMode()
    R.id.menu_choose_apps -> {
    startActivityForResult(Intent(this, AppManagerActivity::class.java), REQUEST_VPN_APP_SELECT)
    }
    R.id.menu_exit -> doExit()
    R.id.menu_log -> showLog()
    R.id.menu_v3_onion_services -> startActivity(Intent(this, OnionServiceActivity::class.java))
    R.id.menu_v3_onion_client_auth -> startActivity(Intent(this, ClientAuthActivity::class.java))
    R.id.menu_settings -> startActivityForResult(SettingsPreferencesActivity.createIntent(this, R.xml.preferences), REQUEST_CODE_SETTINGS)
    R.id.menu_faq -> Toast.makeText(this, "TODO FAQ not implemented...", Toast.LENGTH_LONG).show()
    R.id.menu_about -> {
    AboutDialogFragment()
    .show(supportFragmentManager, AboutDialogFragment.TAG)
    //closeDrawer()
    }
    else -> {}
    }
    true
    }

    }**/
}