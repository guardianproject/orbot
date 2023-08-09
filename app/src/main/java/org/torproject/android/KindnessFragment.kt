package org.torproject.android

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView

import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

import org.torproject.android.service.OrbotConstants
import org.torproject.android.service.OrbotService
import org.torproject.android.service.util.Prefs


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [KindnessFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class KindnessFragment : Fragment() {

    private lateinit var tvAlltimeTotal : TextView
    private lateinit var tvWeeklyTotal : TextView
    private lateinit var swVolunteerMode : SwitchCompat
    private lateinit var btnActionActivate : Button
    private lateinit var pnlActivate : View
    private lateinit var pnlStatus : View

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_kindness, container, false)
        tvAlltimeTotal = view.findViewById(R.id.tvAlltimeTotal)
        tvWeeklyTotal = view.findViewById(R.id.tvWeeklyTotal)
        swVolunteerMode = view.findViewById(R.id.swVolunteerMode)
        btnActionActivate = view.findViewById(R.id.btnActionActivate)
        pnlActivate = view.findViewById(R.id.panel_kindness_activate)
        pnlStatus = view.findViewById(R.id.panel_kindness_status)
        tvAlltimeTotal.text = Prefs.getSnowflakesServed().toString()
        tvWeeklyTotal.text = (Prefs.getSnowflakesServedWeekly()).toString()

        swVolunteerMode.isChecked = Prefs.beSnowflakeProxy()
        swVolunteerMode.setOnCheckedChangeListener { _, isChecked ->
            Prefs.setBeSnowflakeProxy(isChecked)
            showPanelStatus(isChecked)
            sendIntentToService(
                Intent(requireActivity(), OrbotService::class.java)
                    .setAction(OrbotConstants.CMD_SNOWFLAKE_PROXY)
            )
        }

        view.findViewById<TextView>(R.id.swVolunteerAdjust).setOnClickListener {

            KindessConfigBottomSheet().show(requireActivity().supportFragmentManager, CustomBridgeBottomSheet.TAG)

        }

        btnActionActivate.setOnClickListener {
            //   Prefs.setBeSnowflakeProxy(true)
            //  showPanelStatus(true)
            swVolunteerMode.isChecked = true
            Prefs.setBeSnowflakeProxy(true)
            sendIntentToService(OrbotConstants.CMD_ACTIVE)
        }

        showPanelStatus(Prefs.beSnowflakeProxy())

        return view
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment KindnessFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            KindnessFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    private fun sendIntentToService(intent: Intent) = ContextCompat.startForegroundService(requireContext(), intent)
    private fun sendIntentToService(action: String) = sendIntentToService(
        Intent(
            requireContext(),
            OrbotService::class.java
        ).apply {
        this.action = action
    })

    private fun showPanelStatus(isActivated: Boolean) {

        val duration = 250L

        if (isActivated) {

            pnlActivate.animate()
                .alpha(0f)
                .setDuration(duration)
                .withEndAction {
                    pnlActivate.visibility = View.GONE
                }

            pnlStatus.visibility = View.VISIBLE
            pnlStatus.animate()
                .alpha(1f)
                .setDuration(duration)
                .withEndAction {

                }


        }
        else
        {
            pnlActivate.visibility = View.VISIBLE
            pnlActivate.animate()
                .alpha(1f)
                .setDuration(duration)
                .withEndAction {

                }


            pnlStatus.animate()
                .alpha(0f)
                .setDuration(duration)
                .withEndAction {
                    pnlStatus.visibility = View.GONE
                }

        }
    }
}