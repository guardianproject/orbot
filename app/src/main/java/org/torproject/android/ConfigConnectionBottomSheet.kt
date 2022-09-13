package org.torproject.android

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CompoundButton
import android.widget.RadioButton
import org.torproject.android.service.util.Prefs

class ConfigConnectionBottomSheet(private val callbacks: ConnectionHelperCallbacks) : OrbotBottomSheetDialogFragment() {

    private lateinit var rbSmart: RadioButton
    private lateinit var rbDirect: RadioButton
    private lateinit var rbSnowflake: RadioButton
    private lateinit var rbRequestBridge: RadioButton
    private lateinit var rbCustom: RadioButton

    private lateinit var btnAction: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.config_connection_bottom_sheet, container, false)

        rbSmart = v.findViewById(R.id.rbSmart)
        rbDirect = v.findViewById(R.id.rbDirect)
        rbSnowflake = v.findViewById(R.id.rbSnowflake)
        rbRequestBridge = v.findViewById(R.id.rbRequest)
        rbCustom = v.findViewById(R.id.rbCustom)

        val tvSmartSubtitle = v.findViewById<View>(R.id.tvSmartSubtitle)
        val tvDirectSubtitle = v.findViewById<View>(R.id.tvDirectSubtitle)
        val tvSnowflakeSubtitle = v.findViewById<View>(R.id.tvSnowflakeSubtitle)
        val tvRequestSubtitle = v.findViewById<View>(R.id.tvRequestSubtitle)
        val tvCustomSubtitle = v.findViewById<View>(R.id.tvCustomSubtitle)

        val subtitles = arrayListOf(tvSmartSubtitle, tvDirectSubtitle, tvSnowflakeSubtitle,
            tvRequestSubtitle, tvCustomSubtitle)

        val radios = arrayListOf(rbSmart, rbDirect, rbSnowflake, rbRequestBridge, rbCustom)
        val radioSubtitleMap = mapOf<CompoundButton, View>(rbSmart to tvSmartSubtitle,
            rbDirect to tvDirectSubtitle, rbSnowflake to tvSnowflakeSubtitle,
            rbRequestBridge to tvRequestSubtitle, rbCustom to tvCustomSubtitle)
        val allSubtitles = arrayListOf(tvSmartSubtitle, tvDirectSubtitle, tvSnowflakeSubtitle,
            tvRequestSubtitle, tvCustomSubtitle)
        btnAction = v.findViewById(R.id.btnAction)

        // setup containers so radio buttons can be checked if labels are clicked on
        v.findViewById<View>(R.id.smartContainer).setOnClickListener {rbSmart.isChecked = true}
        v.findViewById<View>(R.id.directContainer).setOnClickListener {rbDirect.isChecked = true}
        v.findViewById<View>(R.id.snowflakeContainer).setOnClickListener {rbSnowflake.isChecked = true}
        v.findViewById<View>(R.id.requestContainer).setOnClickListener {rbRequestBridge.isChecked = true}
        v.findViewById<View>(R.id.customContainer).setOnClickListener {rbCustom.isChecked = true}
        v.findViewById<View>(R.id.tvCancel).setOnClickListener { dismiss() }

        rbSmart.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                nestedRadioButtonKludgeFunction(buttonView as RadioButton, radios)
                radioSubtitleMap[buttonView]?.let { onlyShowActiveSubtitle(it, allSubtitles) }
            }
        }
        rbDirect.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                nestedRadioButtonKludgeFunction(buttonView as RadioButton, radios)
                radioSubtitleMap[buttonView]?.let { onlyShowActiveSubtitle(it, allSubtitles) }
            }
        }
        rbSnowflake.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                nestedRadioButtonKludgeFunction(buttonView as RadioButton, radios)
                radioSubtitleMap[buttonView]?.let { onlyShowActiveSubtitle(it, allSubtitles) }
            }
        }
        rbRequestBridge.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                nestedRadioButtonKludgeFunction(buttonView as RadioButton, radios)
                radioSubtitleMap[buttonView]?.let { onlyShowActiveSubtitle(it, allSubtitles) }
                btnAction.text = getString(R.string.next)
            } else {
                btnAction.text = getString(R.string.connect)
            }
        }
        rbCustom.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                nestedRadioButtonKludgeFunction(buttonView as RadioButton, radios)
                radioSubtitleMap[buttonView]?.let { onlyShowActiveSubtitle(it, allSubtitles) }
                btnAction.text = getString(R.string.next)
            } else {
                btnAction.text = getString(R.string.connect)
            }
        }

        selectRadioButtonFromPreference()

        btnAction.setOnClickListener {
            if (rbRequestBridge.isChecked) {
                MoatBottomSheet(callbacks).show(requireActivity().supportFragmentManager, MoatBottomSheet.TAG)
            }
            else if (rbSmart.isChecked) {
                Prefs.putConnectionPathway(Prefs.PATHWAY_SMART)
                closeAndConnect()
            } else if (rbDirect.isChecked) {
                Prefs.putConnectionPathway(Prefs.PATHWAY_DIRECT)
                closeAndConnect()
            } else if (rbSnowflake.isChecked) { // todo which snowflake amp or ...
                Prefs.putConnectionPathway(Prefs.PATHWAY_SNOWFLAKE)
                closeAndConnect()
            } else if (rbCustom.isChecked) {
                CustomBridgeBottomSheet(callbacks).show(requireActivity().supportFragmentManager, CustomBridgeBottomSheet.TAG)
            }
        }

        return v
    }

    private fun closeAndConnect() {
        closeAllSheets()
        callbacks.tryConnecting()
    }

    // it's 2022 and android makes you do ungodly things for mere radio button functionality
    private fun nestedRadioButtonKludgeFunction(rb: RadioButton, all: List<RadioButton>) =
        all.forEach { if (it != rb) it.isChecked = false }

    private fun onlyShowActiveSubtitle(showMe: View, all: List<View>) = all.forEach {
            if (it == showMe) it.visibility = View.VISIBLE
            else it.visibility = View.GONE
        }

    private fun selectRadioButtonFromPreference() {
        val pref = Prefs.getConnectionPathway()
        if (pref.equals(Prefs.PATHWAY_SMART)) rbSmart.isChecked = true
        if (pref.equals(Prefs.PATHWAY_CUSTOM)) rbCustom.isChecked = true
        if (pref.equals(Prefs.PATHWAY_SNOWFLAKE)) rbSnowflake.isChecked = true
        if (pref.equals(Prefs.PATHWAY_DIRECT)) rbDirect.isChecked = true
    }

}