package org.torproject.android

import IPtProxy.IPtProxy
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.android.volley.toolbox.Volley
import org.torproject.android.service.OrbotService
import org.torproject.android.ui.onboarding.ProxiedHurlStack
import java.io.File

class MoatBottomSheet: OrbotBottomSheetDialogFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v =  inflater.inflate(R.layout.moat_bottom_sheet, container, false)
        setupMoat()
        return v
    }

    private fun setupMoat() {
        val fileCacheDir = File(requireActivity().cacheDir, "pt")
        if (!fileCacheDir.exists()) {
            fileCacheDir.mkdir()
        }

        IPtProxy.setStateLocation(fileCacheDir.absolutePath)

        IPtProxy.startObfs4Proxy("DEBUG", false, false, null)

        val phs = ProxiedHurlStack(
            "127.0.0.1", IPtProxy.meekPort().toInt(),
            "url=" + OrbotService.getCdnFront("moat-url")
                    + ";front=" + OrbotService.getCdnFront("moat-front"), "\u0000"
        )

        val queue = Volley.newRequestQueue(requireActivity(), phs)


    }
}