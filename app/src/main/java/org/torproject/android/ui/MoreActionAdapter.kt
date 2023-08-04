package org.torproject.android.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import org.torproject.android.R
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

            tvAction.visibility = View.GONE
            imgView.visibility = View.VISIBLE
            imgView.setImageResource(model.imgId)

            returnView.findViewById<TextView>(R.id.tvLabel).text = context.getString(model.textId)
            returnView.setOnClickListener { model.action() }
        }
        return returnView
    }

}