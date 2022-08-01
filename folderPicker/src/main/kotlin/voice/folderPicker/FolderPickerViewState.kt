package voice.folderPicker

import android.net.Uri

data class FolderPickerViewState(
  val explanationCard: String?,
  val items: List<Item>,
) {

  data class Item(
    val name: String,
    val id: Uri,
  )
}
