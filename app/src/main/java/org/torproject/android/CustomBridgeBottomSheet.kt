package org.torproject.android

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import org.torproject.android.service.util.Prefs

class CustomBridgeBottomSheet(private val callbacks: ConnectionHelperCallbacks) :
    OrbotBottomSheetDialogFragment() {
    companion object {
        const val TAG = "CustomBridgeBottomSheet"
        private val bridgeStatement = Regex("(obfs4|meek|webtunnel)")
        private val obfs4Regex = Regex("""^obfs4\s+(\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}|\[[0-9a-fA-F:]+]):\d+\s+[A-F0-9]{40}(\s+cert=[a-zA-Z0-9+/=]+)?(\s+iat-mode=\d+)?$""")
        private val webtunnelRegex = Regex("""^webtunnel\s+(\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}|\[[0-9a-fA-F:]+]):\d+\s+[A-F0-9]{40}(\s+url=https?://\S+)?(\s+ver=\d+\.\d+\.\d+)?$""")

        fun isValidBridge(input: String): Boolean {
            return input.lines().all { it.matches(obfs4Regex) || it.matches(webtunnelRegex) }
        }
    }

    private lateinit var btnAction: Button
    private lateinit var etBridges: EditText

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.custom_bridge_bottom_sheet, container, false)
        v.findViewById<View>(R.id.tvCancel).setOnClickListener { dismiss() }

        btnAction = v.findViewById(R.id.btnAction)
        btnAction.setOnClickListener {
            Prefs.setBridgesList(etBridges.text.toString())
            closeAllSheets()
            callbacks.tryConnecting()
        }

        etBridges = v.findViewById(R.id.etBridges)
        configureMultilineEditTextScrollEvent(etBridges)

        var bridges = Prefs.getBridgesList()
        if (!bridges.contains(bridgeStatement)) bridges = ""
        etBridges.setText(bridges)

        etBridges.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                updateUi()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        updateUi()
        return v
    }

    private fun updateUi() {
        val inputText = etBridges.text.toString()
        btnAction.isEnabled = inputText.isNotEmpty() && isValidBridge(inputText)

        if (!isValidBridge(inputText)) {
            etBridges.error = "Invalid bridge format"
        } else {
            etBridges.error = null
        }
    }
}
