package voice.features.folderPicker.selectType

import kotlin.time.Duration

internal data class SelectFolderTypeViewState(
  val folderName: String,
  val loading: Boolean,
  val selectedMode: FolderMode,
  val guessedMode: FolderMode?,
  val books: List<Book>,
  val options: List<Option>,
  val editing: Boolean,
  val onboarding: Boolean,
) {

  data class Book(
    val name: String,
    val author: String?,
    val fileCount: Int,
    // folders like "CD 1" and "CD 2" that become one book
    val partCount: Int,
    // sub folders that look like books of their own
    val possibleBookCount: Int,
    // known once its files are analyzed, which also replaces the name and author with the ones the library will show
    val duration: Duration? = null,
    val analyzing: Boolean = false,
  )

  data class Option(
    val mode: FolderMode,
    val books: List<Book>,
  )
}
