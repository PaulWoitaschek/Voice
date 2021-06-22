package de.ph1b.audiobook.features.folderOverview

import android.view.ViewGroup
import de.ph1b.audiobook.R
import de.ph1b.audiobook.features.folderChooser.FolderChooserActivity
import de.ph1b.audiobook.uitools.ExtensionsHolder
import kotlinx.android.synthetic.main.activity_folder_overview_row_layout.*

class FolderOverviewHolder(
  parent: ViewGroup,
  itemClicked: (position: Int) -> Unit
) : ExtensionsHolder(parent, R.layout.activity_folder_overview_row_layout) {

  private val context = parent.context

  init {
    remove.setOnClickListener {
      if (adapterPosition != -1) {
        itemClicked(adapterPosition)
      }
    }
  }

  fun bind(model: FolderModel) {
    // set text
    textView.text = model.folder

    // set correct image
    val drawableId =
      if (model.isCollection == FolderChooserActivity.OperationMode.COLLECTION_BOOK) R.drawable.folder_multiple else if (model.isCollection == FolderChooserActivity.OperationMode.SINGLE_BOOK) R.drawable.ic_folder else R.drawable.folders_multiple

    icon.setImageResource(drawableId)

    // set content description
    val contentDescriptionId =
      if (model.isCollection == FolderChooserActivity.OperationMode.COLLECTION_BOOK) R.string.folder_add_collection else if (model.isCollection == FolderChooserActivity.OperationMode.SINGLE_BOOK) R.string.folder_add_single_book else R.string.folder_add_collections
    val contentDescription = itemView.context.getString(contentDescriptionId)
    icon.contentDescription = contentDescription
  }
}
