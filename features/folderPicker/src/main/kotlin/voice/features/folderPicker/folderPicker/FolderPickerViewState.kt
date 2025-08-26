package voice.features.folderPicker.folderPicker

import android.net.Uri
import voice.core.data.folders.FolderType

data class FolderPickerViewState(val items: List<Item>) {

  data class Item(
    val name: String,
    val id: Uri,
    val folderType: FolderType,
  ) : Comparable<Item> {
    override fun compareTo(other: Item): Int {
      return compareValuesBy(this, other, { it.folderType }, { it.name })
    }
  }
}
