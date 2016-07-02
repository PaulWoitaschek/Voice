package de.ph1b.audiobook.features.folder_overview

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import de.ph1b.audiobook.R
import de.ph1b.audiobook.misc.drawable
import kotlinx.android.synthetic.main.activity_folder_overview_row_layout.view.*

class FolderOverviewAdapter(private val bookCollections: MutableList<String>,
                            private val singleBooks: MutableList<String>,
                            private val deleteClicked: (toDelete: String) -> Unit) :
        RecyclerView.Adapter<FolderOverviewAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.activity_folder_overview_row_layout, parent, false)
        return ViewHolder(v) {
            deleteClicked(getItem(it))
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val file = getItem(position)
        val isCollection = bookCollections.contains(file)
        holder.bind(file, isCollection)
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemCount(): Int {
        return bookCollections.size + singleBooks.size
    }

    private fun getItem(position: Int): String {
        if (bookCollections.size > position) {
            return bookCollections[position]
        } else {
            return singleBooks[position - bookCollections.size]
        }
    }

    interface OnFolderMoreClickedListener {
        fun onFolderMoreClicked(position: Int)
    }

    class ViewHolder(itemView: View, itemClicked: (position: Int) -> Unit) : RecyclerView.ViewHolder(itemView) {

        init {
            itemView.remove.setOnClickListener { itemClicked(adapterPosition) }
        }

        fun bind(text: String, isCollection: Boolean) {
            // set text
            itemView.textView.text = text

            // set correct image
            val drawableId = if (isCollection) R.drawable.folder_multiple else R.drawable.ic_folder
            val drawable = itemView.context.drawable(drawableId)
            itemView.icon.setImageDrawable(drawable)

            // set content description
            val contentDescriptionId = if (isCollection) R.string.folder_add_collection else R.string.folder_add_single_book
            val contentDescription = itemView.context.getString(contentDescriptionId)
            itemView.icon.contentDescription = contentDescription
        }
    }
}
