package de.ph1b.audiobook.features.folderOverview

import android.view.ViewGroup
import de.ph1b.audiobook.R
import de.ph1b.audiobook.databinding.ActivityFolderOverviewRowLayoutBinding
import de.ph1b.audiobook.uitools.ViewBindingHolder

class FolderOverviewHolder(
  parent: ViewGroup,
  itemClicked: (position: Int) -> Unit
) : ViewBindingHolder<ActivityFolderOverviewRowLayoutBinding>(parent, ActivityFolderOverviewRowLayoutBinding::inflate) {

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
    val drawableId = when (model.type) {
      FolderModel.FOLDER_NO_COLLECTION -> R.drawable.ic_folder
      FolderModel.FOLDER_COLLECTION -> R.drawable.folder_multiple
      FolderModel.FOLDER_RECURSIVE -> R.drawable.folder_recursive
      else -> R.drawable.ic_folder
    }
    binding.icon.setImageResource(drawableId)

    // set content description
    val contentDescriptionId = when (model.type) {
      FolderModel.FOLDER_NO_COLLECTION -> R.string.folder_add_single_book
      FolderModel.FOLDER_COLLECTION -> R.string.folder_add_collection
      FolderModel.FOLDER_RECURSIVE -> R.string.folder_add_recursive_book
      else -> R.string.folder_add_single_book
    }

    val contentDescription = itemView.context.getString(contentDescriptionId)
    binding.icon.contentDescription = contentDescription
  }
}
