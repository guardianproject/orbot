package org.torproject.android

import android.content.Context
import android.content.pm.PackageManager
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import org.torproject.android.service.OrbotConstants
import org.torproject.android.service.util.Prefs
import org.torproject.android.service.util.Utils
import org.torproject.android.ui.OrbotMenuAction
import org.torproject.android.ui.v3onionservice.PermissionManager
import java.util.*

class OrbotMenuActionAdapter(context: Context, list: ArrayList<OrbotMenuAction>) : ArrayAdapter<OrbotMenuAction>(context, R.layout.action_list_view, list) {

    private val layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val returnView = convertView ?: layoutInflater.inflate(R.layout.action_list_view, null)
        getItem(position)?.let { model ->
            val imgView = returnView.findViewById<ImageView>(R.id.ivAction)
            val tvAction  = returnView.findViewById<TextView>(R.id.tvEmoji)
            val hvApps = returnView.findViewById<HorizontalScrollView>(R.id.llBoxShortcuts)

            if (model.imgId == R.drawable.ic_choose_apps) {
                tvAction.visibility = View.GONE
                imgView.visibility = View.VISIBLE
                imgView.setImageResource(model.imgId)
                drawAppShortcuts(hvApps)

            }
            else if (model.imgId == 0) {
                imgView.visibility = View.GONE
                val exit = Prefs.getExitNodes()
                if (exit.length == 2) tvAction.text = Utils.convertCountryCodeToFlagEmoji(exit)
                else tvAction.text = context.getString(R.string.globe)
                tvAction.visibility = View.VISIBLE
            } else {
                tvAction.visibility = View.GONE
                imgView.visibility = View.VISIBLE
                imgView.setImageResource(model.imgId)
            }
            returnView.findViewById<TextView>(R.id.tvLabel).text = context.getString(model.textId)
            returnView.setOnClickListener { model.action() }
        }
        return returnView
    }

    private fun drawAppShortcuts(llBoxShortcuts: HorizontalScrollView) {

        val tordAppString = Prefs.getSharedPrefs(context)
            .getString(OrbotConstants.PREFS_KEY_TORIFIED, "")
        if (!TextUtils.isEmpty(tordAppString)) {

            val packageManager: PackageManager = context.getPackageManager()
            val tordApps = tordAppString!!.split("|").toTypedArray()
            val container = llBoxShortcuts.getChildAt(0) as LinearLayout

            llBoxShortcuts.visibility = View.VISIBLE
            container.removeAllViews()
            val icons: MutableMap<String, ImageView> = TreeMap()
            for (tordApp in tordApps) {

                if (tordApp.isNotEmpty()) {
                    try {
                        packageManager.getPackageInfo(tordApp, 0)
                        val iv = ImageView(context)
                        val applicationInfo = packageManager.getApplicationInfo(tordApp, 0)
                        iv.setImageDrawable(packageManager.getApplicationIcon(tordApp))
                        val params = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        params.height = 100
                        params.width = 100
                        params.setMargins(1, 20, 1, 1)
                        iv.layoutParams = params
                        /**
                        iv.setOnClickListener { v: View? ->
                        openBrowser(
                        OrbotMainActivity.URL_TOR_CHECK,
                        false,
                        tordApp
                        )
                        }**/
                        icons[packageManager.getApplicationLabel(applicationInfo).toString()] = iv
                    } catch (e: PackageManager.NameNotFoundException) {
                        //couldn't draw icon for the package name
                        Log.d("Orbot", "error getting package info for: " + tordApp);

                    }
                }
            }
            if (icons.isNotEmpty()) {
                val sorted = TreeMap(icons)
                for (iv in sorted.values) container.addView(iv)
            }
        }
    }

}