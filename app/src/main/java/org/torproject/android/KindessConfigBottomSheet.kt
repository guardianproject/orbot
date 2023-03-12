package org.torproject.android

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CompoundButton
import android.widget.EditText
import androidx.appcompat.widget.SwitchCompat
import org.torproject.android.service.util.Prefs

class KindessConfigBottomSheet(): OrbotBottomSheetDialogFragment() {
    companion object {
        const val TAG = "KindnessConfig"
    }

    private lateinit var btnAction: Button
    private lateinit var etBridges: EditText

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v =  inflater.inflate(R.layout.kindess_config_bottom_sheet, container, false)
        v.findViewById<View>(R.id.tvCancel).setOnClickListener { dismiss() }
        btnAction = v.findViewById(R.id.btnAction)

        var configWifi = v.findViewById<SwitchCompat>(R.id.swKindnessConfigWifi)
        var configCharging = v.findViewById<SwitchCompat>(R.id.swKindnessConfigCharging)

        btnAction.setOnClickListener {

            Prefs.setBeSnowflakeProxyLimitWifi(configWifi.isChecked)
            Prefs.setBeSnowflakeProxyLimitCharging(configCharging.isChecked)

            closeAllSheets()
        }

        configWifi.isChecked = Prefs.limitSnowflakeProxyingWifi()
        configWifi.setOnCheckedChangeListener { compoundButton: CompoundButton, b: Boolean ->
        }

        configCharging.isChecked = Prefs.limitSnowflakeProxyingCharging()
        configCharging.setOnCheckedChangeListener { compoundButton: CompoundButton, b: Boolean ->
        }



        return v
    }



}