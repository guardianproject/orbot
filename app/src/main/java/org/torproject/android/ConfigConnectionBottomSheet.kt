package org.torproject.android

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CompoundButton
import android.widget.RadioButton
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ConfigConnectionBottomSheet : BottomSheetDialogFragment() {

    private lateinit var rbSmart: RadioButton
    private lateinit var rbDirect: RadioButton
    private lateinit var rbSnowflake: RadioButton
    private lateinit var rbRequestBridge: RadioButton
    private lateinit var rbCustom: RadioButton

    private lateinit var btnAction: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v =  inflater.inflate(R.layout.config_connection_bottom_sheet, container, false)

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
        val radioSubtitleMap = mapOf<CompoundButton, View>(
            rbSmart to tvSmartSubtitle,
            rbDirect to tvDirectSubtitle,
            rbSnowflake to tvSnowflakeSubtitle,
            rbRequestBridge to tvRequestSubtitle,
            rbCustom to tvCustomSubtitle)
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
            }
        }

        // todo for now just assume the preference is set to the smart connect RB
        rbSmart.isChecked = true

        return v
    }

    // it's 2022 and android makes you do ungodly things for mere radio button functionality
    private fun nestedRadioButtonKludgeFunction(rb: RadioButton, all: List<RadioButton>) {
        for (radio in all) {
            if (radio == rb) continue
            radio.isChecked = false
        }
    }

    private fun onlyShowActiveSubtitle(showMe: View, all: List<View>) {
        for (tv in all) {
            if (tv == showMe) tv.visibility = View.VISIBLE
            else tv.visibility = View.GONE
        }
    }

}