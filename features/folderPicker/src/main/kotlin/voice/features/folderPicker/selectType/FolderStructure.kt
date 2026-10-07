package voice.features.folderPicker.selectType

import voice.core.data.audioFileCount
import voice.core.data.isAudioFile
import voice.core.documentfile.CachedDocumentFile
import voice.core.documentfile.nameWithoutExtension

/**
 * Guesses how the books in this folder are organized, looking at its first two levels:
 * - Only folders like "CD 1" and "CD 2", or mostly loose chapter files: one book.
 * - Mostly folders that hold further book folders: authors (or series) with their books.
 * - Otherwise: a library where every folder or audiobook file is a book.
 */
internal fun CachedDocumentFile.guessFolderMode(): FolderMode {
  val audioFiles = children.filter { it.isAudioFile() }
  val folders = audioFolders()
  if (folders.isEmpty()) {
    val wholeBooks = audioFiles.size > 1 && audioFiles.all { it.looksLikeWholeBook() }
    return if (wholeBooks) FolderMode.Audiobooks else FolderMode.SingleBook
  }
  if (folders.all { it.isPartFolder() }) {
    return FolderMode.SingleBook
  }
  val looseChapters = audioFiles.count { !it.looksLikeWholeBook() }
  if (looseChapters > folders.size) {
    return FolderMode.SingleBook
  }
  val authorFolders = folders.count { it.looksLikeAuthor() }
  return if (authorFolders * 2 > folders.size) FolderMode.Authors else FolderMode.Audiobooks
}

/**
 * The books Voice finds in this folder in the given [mode]. This mirrors what the scanner does.
 */
internal fun CachedDocumentFile.books(mode: FolderMode): List<SelectFolderTypeViewState.Book> {
  return when (mode) {
    FolderMode.SingleBook -> listOf(toBook(author = null))
    FolderMode.Audiobooks -> children.map { it.toBook(author = null) }
    FolderMode.Authors -> children.flatMap { author ->
      if (author.isFile) {
        listOf(author.toBook(author = null))
      } else {
        val authorName = author.nameWithoutExtension()
        author.children.map { it.toBook(author = authorName) }
      }
    }
  }.filter { it.fileCount > 0 }
}

private fun CachedDocumentFile.toBook(author: String?): SelectFolderTypeViewState.Book {
  val (parts, books) = audioFolders().partition { it.isPartFolder() }
  return SelectFolderTypeViewState.Book(
    name = nameWithoutExtension(),
    author = author,
    fileCount = audioFileCount(),
    partCount = parts.size,
    // a single sub folder is often just bonus material
    possibleBookCount = books.size.takeIf { it > 1 } ?: 0,
  )
}

private fun CachedDocumentFile.audioFolders(): List<CachedDocumentFile> {
  return children.filter { it.isDirectory && it.audioFileCount() > 0 }
}

// an author folder only holds book folders
private fun CachedDocumentFile.looksLikeAuthor(): Boolean {
  return children.none { it.isAudioFile() } && audioFolders().any { !it.isPartFolder() }
}

private fun CachedDocumentFile.looksLikeWholeBook(): Boolean {
  return name.orEmpty().endsWith(".m4b", ignoreCase = true) || length > WHOLE_BOOK_MIN_BYTES
}

private fun CachedDocumentFile.isPartFolder(): Boolean = partFolderName.matches(name.orEmpty().trim())

// "CD 1", "Disc 02", "My Book - CD2", "Part 3", "Teil 1", "Vol. 2" or just "01"
private val partFolderName = Regex(
  """(?i)^((.*[\s._-])?(cd|disc|disk|tape|side)|part|teil|vol\.?|volume)?[\s._-]*\d{1,3}$""",
)

private const val WHOLE_BOOK_MIN_BYTES = 200_000_000L
