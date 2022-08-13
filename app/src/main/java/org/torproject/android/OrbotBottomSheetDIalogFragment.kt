package org.torproject.android

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/*
Class to setup default bottom sheet behavior for Config Connection, MOAT and any other
bottom sheets to come
 */
open class OrbotBottomSheetDialogFragment : BottomSheetDialogFragment() {
    private var backPressed = false
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val dialog = object : BottomSheetDialog(requireActivity(), theme) {
                override fun onBackPressed() {
                    super.onBackPressed()
                    Toast.makeText(requireActivity(), "Bcak Pressed", Toast.LENGTH_LONG).show()
                    backPressed = true
                }
            }
            dialog.setOnShowListener {setupRatio(dialog)}
//            dialog.setOnCancelListener {Toast.makeText(requireActivity(), "Cancelled", Toast.LENGTH_LONG).show()}
            return dialog
//            return super.onCreateDialog(savedInstanceState).apply {
//            setOnShowListener {
//                setupRatio(it as BottomSheetDialog)
//            }
//        }
    }

    override fun onCancel(dialog: DialogInterface) {
//        super.onCancel(dialog)
        if (!backPressed) {
            // todo this method only works for now because OrbotActivity is locked in portrait mode
            Toast.makeText(requireActivity(), "Touched Outside", Toast.LENGTH_LONG).show()
            val fm = requireActivity().supportFragmentManager
            for (f in fm.fragments) {
                if (f == this) continue
                fm.beginTransaction().remove(f).commit()
            }
        }
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