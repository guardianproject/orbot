package org.torproject.android.ui.v3onionservice

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.view.View

import androidx.fragment.app.FragmentActivity

import com.google.android.material.snackbar.Snackbar

import org.torproject.android.R

object PermissionManager {
    private const val SNACK_BAR_DURATION = 5000

    @JvmStatic
    fun requestBatteryPermissions(activity: FragmentActivity, view: View) {
        val packageName = activity.packageName
        val pm = activity.applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager

        if (pm.isIgnoringBatteryOptimizations(packageName)) {
            return
        }

        Snackbar.make(view, R.string.consider_disable_battery_optimizations,
            SNACK_BAR_DURATION).setAction(R.string.disable) {
            val intent = Intent().apply {
                action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                data = Uri.parse("package:$packageName")
            }
            activity.startActivity(intent)
        }.show()
    }

    @JvmStatic
    fun requestDropBatteryPermissions(activity: FragmentActivity, view: View) {
        val pm = activity.applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(activity.packageName)) {
            return
        }

        Snackbar.make(view, R.string.consider_enable_battery_optimizations,
            SNACK_BAR_DURATION).setAction(R.string.enable) {
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            activity.startActivity(intent)
        }.show()
    }
}
