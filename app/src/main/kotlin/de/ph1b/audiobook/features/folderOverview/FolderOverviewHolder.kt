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
      if (absoluteAdapterPosition != -1) {
        itemClicked(absoluteAdapterPosition)
      }
    }
  }

  fun bind(model: FolderModel) {
    // set text
    binding.textView.text = model.folder

    // set correct image
    val drawableId = if (model.isCollection) R.drawable.folder_multiple else R.drawable.ic_folder
    binding.icon.setImageResource(drawableId)

    // set content description
    val contentDescriptionId =
      if (model.isCollection) R.string.folder_add_collection else R.string.folder_add_single_book
    val contentDescription = itemView.context.getString(contentDescriptionId)
    binding.icon.contentDescription = contentDescription
  }
}
