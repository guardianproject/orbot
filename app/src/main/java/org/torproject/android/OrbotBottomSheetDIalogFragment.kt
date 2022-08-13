package org.torproject.android

import android.app.Dialog
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.View
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/*
Class to setup default bottom sheet behavior for Config Connection, MOAT and any other
bottom sheets to come
 */
open class OrbotBottomSheetDialogFragment : BottomSheetDialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setOnShowListener {
            setupRatio(it as BottomSheetDialog)
        }
        return dialog
    }

    private fun setupRatio(bsd : BottomSheetDialog) {
        val bottomSheet = bsd.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheet?.let {
            val behavior = BottomSheetBehavior.from(it)
            val layoutParams = it.layoutParams
            layoutParams.height = getHeight()
            bottomSheet.layoutParams = layoutParams
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    private fun getHeight() : Int{
        // todo handle bigger device heights
        val displayMetrics = DisplayMetrics()
        requireActivity().windowManager.defaultDisplay.getMetrics(displayMetrics)
        return displayMetrics.heightPixels * 65 / 100
    }
}