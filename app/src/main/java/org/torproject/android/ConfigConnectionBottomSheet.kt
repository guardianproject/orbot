package org.torproject.android

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioButton
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

        val arr = arrayListOf(rbSmart, rbDirect, rbSnowflake, rbRequestBridge, rbCustom)
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
                nestedRadioButtonKludgeFunction(buttonView as RadioButton, arr)
            }
        }
        rbDirect.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                nestedRadioButtonKludgeFunction(buttonView as RadioButton, arr)
            }
        }
        rbSnowflake.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                nestedRadioButtonKludgeFunction(buttonView as RadioButton, arr)
            }
        }
        rbRequestBridge.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                nestedRadioButtonKludgeFunction(buttonView as RadioButton, arr)
            }
        }
        rbCustom.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                nestedRadioButtonKludgeFunction(buttonView as RadioButton, arr)
            }
        }

        return v
    }

    private fun nestedRadioButtonKludgeFunction(rb: RadioButton, all: List<RadioButton>) {
        for (radio in all) {
            if (radio == rb) continue
            radio.isChecked = false
        }
    }

}