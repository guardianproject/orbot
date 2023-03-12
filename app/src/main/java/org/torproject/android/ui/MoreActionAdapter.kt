package org.torproject.android.ui

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import org.torproject.android.R
import org.torproject.android.service.OrbotConstants
import org.torproject.android.service.util.Prefs
import org.torproject.android.service.util.Utils
import java.util.*


class MoreActionAdapter(context: Context, list: ArrayList<OrbotMenuAction>) : ArrayAdapter<OrbotMenuAction>(context,
    R.layout.action_list_view, list) {

    private val layoutInflater =
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val returnView = convertView ?: layoutInflater.inflate(R.layout.action_list_view, null)
        getItem(position)?.let { model ->
            val imgView = returnView.findViewById<ImageView>(R.id.ivAction)
            val tvAction = returnView.findViewById<TextView>(R.id.tvEmoji)
            val hvApps = returnView.findViewById<HorizontalScrollView>(R.id.llBoxShortcuts)

            tvAction.visibility = View.GONE
            imgView.visibility = View.VISIBLE
            imgView.setImageResource(model.imgId)

            returnView.findViewById<TextView>(R.id.tvLabel).text = context.getString(model.textId)
            returnView.setOnClickListener { model.action() }
        }
        return returnView
    }


    private fun openBrowser(checkUrl: String, doSomething: Boolean, packageName: String)
    {
        startIntent(context, packageName, Intent.ACTION_VIEW, Uri.parse(checkUrl));

    }

    private fun startIntent(context: Context, pkg: String, action: String, data: Uri) {
        val i = Intent()
        val pm: PackageManager = context.getPackageManager()
        try {
            /**
             * if (pkg != null) {
             * i = pm.getLaunchIntentForPackage(pkg);
             * if (i == null)
             * throw new PackageManager.NameNotFoundException();
             * } else {
             * i = new Intent();
             * } */
            i.setPackage(pkg)
            i.action = action
            i.data = data
            if (i.resolveActivity(pm) != null) context.startActivity(i)
        } catch (e: Exception) {
            // Should not occur. Ignore.
        }
    }

}