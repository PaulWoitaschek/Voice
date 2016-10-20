package de.ph1b.audiobook.uitools

/**
 *Created by Timur on 20.10.2016.
 */

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

import com.google.android.gms.drive.Metadata
import com.google.android.gms.drive.widget.DataBufferAdapter

class ResultsAdapter(p0: Context?, p1: Int) : DataBufferAdapter<Metadata>(p0, p1) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var convertView = convertView
        if (convertView == null) {
            convertView = View.inflate(context,
                    android.R.layout.simple_list_item_1, null)
        }
        val metadata = getItem(position)
        val titleTextView = convertView!!.findViewById(android.R.id.text1) as TextView
        titleTextView.text = metadata.title
        return convertView
    }
}