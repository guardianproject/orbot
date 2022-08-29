package org.torproject.android

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import org.torproject.android.service.util.Prefs
import org.torproject.android.service.util.Utils
import org.torproject.android.ui.OrbotMenuAction

class OrbotMenuActionAdapter(context: Context, list: ArrayList<OrbotMenuAction>) : ArrayAdapter<OrbotMenuAction>(context, R.layout.action_list_view, list) {

    private val layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val returnView = convertView ?: layoutInflater.inflate(R.layout.action_list_view, null)
        getItem(position)?.let { model ->
            val imgView = returnView.findViewById<ImageView>(R.id.ivAction)
            val tvAction  = returnView.findViewById<TextView>(R.id.tvEmoji)
            if (model.imgId == 0) {
                imgView.visibility = View.GONE
                tvAction.text = Utils.convertCountryCodeToFlagEmoji(Prefs.getExitNodes())
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

}