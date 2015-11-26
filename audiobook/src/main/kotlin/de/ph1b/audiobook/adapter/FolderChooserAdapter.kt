package de.ph1b.audiobook.adapter

import android.content.Context
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import de.ph1b.audiobook.R
import de.ph1b.audiobook.activity.FolderChooserActivity
import java.io.File


/**
 * Adapter for displaying files and folders.
 * Constructor that initializes the class with the necessary values
 *
 * @param c    The context
 * @param data The files to show
 * @param mode The operation mode which defines the interaction.
 *
 * @author Paul Woitaschek
 */
class FolderChooserAdapter(private val c: Context, private val data: List<File>, private val mode: FolderChooserActivity.OperationMode) : BaseAdapter() {

    init {
        if (mode !== FolderChooserActivity.OperationMode.SINGLE_BOOK && mode !== FolderChooserActivity.OperationMode.COLLECTION_BOOK) {
            throw IllegalArgumentException("Invalid mode=" + mode)
        }
    }

    override fun getCount(): Int {
        return data.size
    }

    override fun getItem(position: Int): File {
        return data[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var view = convertView
        val viewHolder: ViewHolder
        if (view == null) {
            val li = LayoutInflater.from(c)
            view = li.inflate(R.layout.activity_folder_chooser_adapter_row_layout, parent,
                    false)

            viewHolder = ViewHolder(view)
            view.tag = viewHolder
        } else {
            viewHolder = view.tag as ViewHolder
        }

        val selectedFile = data[position]
        val isDirectory = selectedFile.isDirectory

        viewHolder.textView.text = selectedFile.name

        // if its not a collection its also fine to pick a file
        if (mode === FolderChooserActivity.OperationMode.COLLECTION_BOOK) {
            viewHolder.textView.isEnabled = isDirectory
        }

        val icon = ContextCompat.getDrawable(c, if (isDirectory) R.drawable.ic_folder else R.drawable.ic_audiotrack_white_48dp)
        viewHolder.imageView.setImageDrawable(icon)
        viewHolder.imageView.contentDescription = c.getString(if (isDirectory) R.string.content_is_folder else R.string.content_is_file)

        return view!!
    }

    internal class ViewHolder(parent: View) {
        internal val textView: TextView
        internal val imageView: ImageView

        init {
            textView = parent.findViewById(R.id.singleline_text1) as TextView
            imageView = parent.findViewById(R.id.singleline_image1) as ImageView
        }
    }
}
