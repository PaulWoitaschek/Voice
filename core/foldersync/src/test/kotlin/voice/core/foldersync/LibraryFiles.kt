package voice.core.foldersync

import android.net.Uri
import android.provider.DocumentsContract
import voice.core.data.Book
import voice.core.data.BookContent
import voice.core.data.BookId
import voice.core.data.BookMetadata
import voice.core.data.Chapter
import voice.core.data.ChapterId
import java.time.Instant

private const val AUTHORITY = "com.android.externalstorage.documents"

fun folder(tree: String = "primary:Audiobooks"): Uri = DocumentsContract.buildTreeDocumentUri(AUTHORITY, tree)

fun book(
  tree: String = "primary:Audiobooks",
  path: String = "Book",
  fileSizes: List<Long> = listOf(10, 20),
  currentFile: Int = 0,
  position: Long = 0,
  lastPlayedAt: Long = 0,
  playbackSpeed: Float = 1F,
  customName: String? = null,
): Book {
  val bookUri = DocumentsContract.buildDocumentUriUsingTree(folder(tree), "$tree/$path")
  val chapters = fileSizes.mapIndexed { index, size ->
    val fileUri = DocumentsContract.buildDocumentUriUsingTree(folder(tree), "$tree/$path/0$index.mp3")
    Chapter(
      id = ChapterId(fileUri.toString()),
      name = null,
      duration = 60_000,
      fileLastModified = Instant.EPOCH,
      fileSize = size,
      markData = emptyList(),
    )
  }
  return Book(
    content = BookContent(
      id = BookId(bookUri.toString()),
      playbackSpeed = playbackSpeed,
      skipSilence = false,
      isActive = true,
      lastPlayedAt = Instant.ofEpochMilli(lastPlayedAt),
      addedAt = Instant.EPOCH,
      chapters = chapters.map { it.id },
      currentChapter = chapters[currentFile].id,
      positionInChapter = position,
      gain = 0F,
      metadata = BookMetadata(
        name = path,
        author = null,
        album = null,
        title = null,
        genre = null,
        narrator = null,
        series = null,
        part = null,
        cover = null,
        fileLastModified = null,
      ),
      customName = customName,
      customCover = null,
    ),
    chapters = chapters,
  )
}

internal fun place(
  file: Int = 1,
  position: Long = 3000,
  speed: Float = 1F,
  playedAt: Long = 2000,
): SyncedPlace = SyncedPlace(file = file, position = position, speed = speed, playedAt = playedAt)
