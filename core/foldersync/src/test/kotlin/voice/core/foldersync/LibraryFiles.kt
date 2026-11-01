package voice.core.foldersync

import android.net.Uri
import android.provider.DocumentsContract
import java.time.Instant
import kotlin.uuid.Uuid
import voice.core.data.Book
import voice.core.data.BookContent
import voice.core.data.BookId
import voice.core.data.BookMetadata
import voice.core.data.Bookmark
import voice.core.data.Chapter
import voice.core.data.ChapterId
import voice.core.data.CustomMetadata

private const val AUTHORITY = "com.android.externalstorage.documents"

fun folder(tree: String = "primary:Audiobooks"): Uri =
  DocumentsContract.buildTreeDocumentUri(AUTHORITY, tree)

fun book(
  tree: String = "primary:Audiobooks",
  path: String = "Book",
  fileSizes: List<Long> = listOf(10, 20),
  currentFile: Int = 0,
  position: Long = 0,
  lastPlayedAt: Long = 0,
  playbackSpeed: Float = 1F,
  customName: String? = null,
  author: String? = null,
  customAuthor: String? = null,
  series: String? = null,
  customSeries: String? = null,
  part: String? = null,
  customPart: String? = null,
): Book {
  val bookUri = DocumentsContract.buildDocumentUriUsingTree(folder(tree), "$tree/$path")
  val chapters = fileSizes.mapIndexed { index, size ->
    val fileUri =
      DocumentsContract.buildDocumentUriUsingTree(folder(tree), "$tree/$path/0$index.mp3")
    Chapter(
      id = ChapterId(fileUri.toString()),
      name = null,
      duration = 60_000,
      fileLastModified = Instant.EPOCH,
      fileSize = size,
      markData = emptyList(),
    )
  }
  val id = BookId(bookUri.toString())
  return Book(
    content =
      BookContent(
        id = id,
        playbackSpeed = playbackSpeed,
        skipSilence = false,
        isActive = true,
        lastPlayedAt = Instant.ofEpochMilli(lastPlayedAt),
        addedAt = Instant.EPOCH,
        chapters = chapters.map { it.id },
        currentChapter = chapters[currentFile].id,
        positionInChapter = position,
        gain = 0F,
        metadata =
          BookMetadata(
            name = path,
            author = author,
            album = null,
            title = null,
            genre = null,
            narrator = null,
            series = series,
            part = part,
            cover = null,
            fileLastModified = null,
          ),
      ),
    chapters = chapters,
    custom =
      CustomMetadata(
        id,
        name = customName,
        author = customAuthor,
        series = customSeries,
        part = customPart,
      ),
  )
}

internal fun place(
  file: Int = 1,
  position: Long = 3000,
  speed: Float = 1F,
  playedAt: Long = 2000,
): SyncedPlace = SyncedPlace(file = file, position = position, speed = speed, playedAt = playedAt)

internal fun Book.bookmark(
  file: Int = 1,
  time: Long = 1000,
  title: String? = "Note",
  addedAt: Long = 100,
  id: Uuid = Uuid.parse("00000000-0000-0000-0000-000000000001"),
): Bookmark =
  Bookmark(
    bookId = this.id,
    chapterId = chapters[file].id,
    title = title,
    time = time,
    addedAt = Instant.ofEpochMilli(addedAt),
    id = Bookmark.Id(id),
  )

internal fun Bookmark.synced(
  file: Int = 1,
  changedAt: Long = addedAt.toEpochMilli(),
): SyncedBookmark =
  SyncedBookmark(
    file = file,
    time = time,
    title = title,
    kind = kind,
    addedAt = addedAt.toEpochMilli(),
    changedAt = changedAt,
  )
