package de.ph1b.audiobook.features.folderChooser

import android.view.ViewGroup
import de.ph1b.audiobook.R
import de.ph1b.audiobook.databinding.ActivityFolderChooserAdapterRowLayoutBinding
import de.ph1b.audiobook.uitools.ViewBindingHolder
import java.io.File

class Holder(
  parent: ViewGroup,
  private val mode: FolderChooserActivity.OperationMode,
  private val listener: (selected: File) -> Unit
) : ViewBindingHolder<ActivityFolderChooserAdapterRowLayoutBinding>(parent, ActivityFolderChooserAdapterRowLayoutBinding::inflate) {

  private var boundFile: File? = null

  init {
    itemView.setOnClickListener {
      boundFile?.let(listener)
    }
  }

  fun bind(selectedFile: File) {
    boundFile = selectedFile
    val isDirectory = selectedFile.isDirectory

    binding.text.text = selectedFile.name

    // if its not a collection its also fine to pick a file
    if (mode == FolderChooserActivity.OperationMode.COLLECTION_BOOK) {
      binding.text.isEnabled = isDirectory
    }

    val context = itemView.context
    val drawableRes = if (isDirectory) R.drawable.ic_folder else R.drawable.ic_album
    binding.icon.setImageResource(drawableRes)
    binding.icon.contentDescription = context.getString(
      if (isDirectory) {
        R.string.content_is_folder
      } else R.string.content_is_file
    )
  }
}
