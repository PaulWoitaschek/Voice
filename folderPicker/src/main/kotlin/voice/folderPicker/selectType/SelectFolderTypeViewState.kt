package voice.folderPicker.selectType

internal data class SelectFolderTypeViewState(
  val books: List<Book>,
  val selectedFolderMode: FolderMode,
  val loading: Boolean,
  val noBooksDetected: Boolean,
  val addButtonVisible: Boolean,
) {
  data class Book(
    val name: String,
    val fileCount: Int,
  )
}
