package de.ph1b.audiobook.features.folder_overview

import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import de.ph1b.audiobook.R
import de.ph1b.audiobook.misc.drawable
import de.ph1b.audiobook.misc.find

class FolderOverviewHolder(itemView: View, itemClicked: (position: Int) -> Unit) : RecyclerView.ViewHolder(itemView) {

    private var remove: View = find(R.id.remove)
    private val textView: TextView = find(R.id.textView)
    private val icon: ImageView = find(R.id.icon)

    init {
        remove.setOnClickListener { itemClicked(adapterPosition) }
    }

    fun bind(model: FolderModel) {
        // set text
        textView.text = model.folder

        // set correct image
        val drawableId = if (model.isCollection) R.drawable.folder_multiple else R.drawable.ic_folder
        val drawable = itemView.context.drawable(drawableId)
        icon.setImageDrawable(drawable)

        // set content description
        val contentDescriptionId = if (model.isCollection) R.string.folder_add_collection else R.string.folder_add_single_book
        val contentDescription = itemView.context.getString(contentDescriptionId)
        icon.contentDescription = contentDescription
    }
}