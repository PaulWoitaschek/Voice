package de.ph1b.audiobook.features.folder_chooser

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import de.ph1b.audiobook.R
import de.ph1b.audiobook.misc.drawable
import de.ph1b.audiobook.misc.find
import de.ph1b.audiobook.misc.layoutInflater
import java.io.File
import java.util.*


/**
 * Adapter for displaying files and folders.
 * Constructor that initializes the class with the necessary values
 *
 * @param c    The context
 * @param mode The operation mode which defines the interaction.
 *
 * @author Paul Woitaschek
 */
class FolderChooserAdapter(private val c: Context,
                           private val mode: FolderChooserActivity.OperationMode,
                           private val listener: (selected: File) -> Unit)
: RecyclerView.Adapter<FolderChooserAdapter.Holder>() {

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(data[position])

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = parent.layoutInflater().inflate(R.layout.activity_folder_chooser_adapter_row_layout, parent, false)
        return Holder(view)
    }

    override fun getItemCount() = data.size

    private val data = ArrayList<File>()

    fun newData(newData: List<File>) {
        data.clear()
        data.addAll(newData)
        notifyDataSetChanged()
    }

    inner class Holder(root: View) : RecyclerView.ViewHolder(root) {

        private val textView: TextView = find(R.id.text)
        private val image: ImageView = find(R.id.icon)

        init {
            root.setOnClickListener {
                listener.invoke(data[adapterPosition])
            }
        }

        fun bind(selectedFile: File) {
            val isDirectory = selectedFile.isDirectory

            textView.text = selectedFile.name

            // if its not a collection its also fine to pick a file
            if (mode == FolderChooserActivity.OperationMode.COLLECTION_BOOK) {
                textView.isEnabled = isDirectory
            }

            val icon = c.drawable(if (isDirectory) R.drawable.ic_folder else R.drawable.ic_album)
            image.setImageDrawable(icon)
            image.contentDescription = c.getString(if (isDirectory) R.string.content_is_folder else R.string.content_is_file)
        }
    }
}
