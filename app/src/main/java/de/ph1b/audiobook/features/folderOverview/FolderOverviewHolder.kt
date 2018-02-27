package de.ph1b.audiobook.features.folderOverview

import android.support.v7.widget.RecyclerView
import de.ph1b.audiobook.R
import de.ph1b.audiobook.databinding.ActivityFolderOverviewRowLayoutBinding
import de.ph1b.audiobook.misc.drawable

class FolderOverviewHolder(
  private val binding: ActivityFolderOverviewRowLayoutBinding,
  itemClicked: (position: Int) -> Unit
) : RecyclerView.ViewHolder(binding.root) {

  init {
    binding.remove.setOnClickListener {
      if (adapterPosition != -1) {
        itemClicked(adapterPosition)
      }
    }
  }

  fun bind(model: FolderModel) {
    // set text
    binding.textView.text = model.folder

    // set correct image
    val drawableId = if (model.isCollection) R.drawable.folder_multiple else R.drawable.ic_folder
    val drawable = itemView.context.drawable(drawableId)
    binding.icon.setImageDrawable(drawable)

    // set content description
    val contentDescriptionId =
      if (model.isCollection) R.string.folder_add_collection else R.string.folder_add_single_book
    val contentDescription = itemView.context.getString(contentDescriptionId)
    binding.icon.contentDescription = contentDescription
  }
}
