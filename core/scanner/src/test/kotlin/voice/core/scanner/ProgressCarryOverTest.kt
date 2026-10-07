package voice.core.scanner

import android.provider.DocumentsContract
import androidx.core.net.toUri
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import voice.core.data.BookContent
import voice.core.data.BookId
import voice.core.data.Bookmark
import voice.core.data.ChapterId
import java.io.File
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class ProgressCarryOverTest {

  private val bookmarkRepo = MemoryBookmarkRepo()
  private val currentBookStore = MemoryDataStore<BookId?>(null)
  private val carryOver = ProgressCarryOver(bookmarkRepo, currentBookStore)

  @Test
  fun `a document has the same key in every folder it is opened through`() {
    val throughParent = documentUri(tree = "primary:Audiobooks", document = "primary:Audiobooks/Book/1.mp3")
    val throughBook = documentUri(tree = "primary:Audiobooks/Book", document = "primary:Audiobooks/Book/1.mp3")
    val singleFile = "content://com.android.externalstorage.documents/document/primary%3AAudiobooks%2FBook%2F1.mp3"

    assertEquals(expected = throughParent.documentKey(), actual = throughBook.documentKey())
    assertEquals(expected = throughParent.documentKey(), actual = singleFile.toUri().documentKey())
  }

  @Test
  fun `a book added through another folder keeps its progress, name and bookmarks`() = runTest {
    val oldChapters = listOf("1.mp3", "2.mp3").map {
      ChapterId(documentUri(tree = "primary:Audiobooks", document = "primary:Audiobooks/Book/$it"))
    }
    val old = bookContent(
      id = BookId(documentUri(tree = "primary:Audiobooks", document = "primary:Audiobooks/Book")),
      chapters = oldChapters,
    ).copy(
      name = "Renamed",
      cover = File("cover.webp"),
      currentChapter = oldChapters[1],
      positionInChapter = 42,
      lastPlayedAt = Instant.parse("2026-01-01T10:00:00Z"),
    )
    val bookmark = Bookmark(
      bookId = old.id,
      chapterId = oldChapters[1],
      title = "Twist",
      time = 7,
      addedAt = Instant.EPOCH,
      setBySleepTimer = false,
      id = Bookmark.Id.random(),
    )
    bookmarkRepo.addBookmark(bookmark)
    currentBookStore.updateData { old.id }

    val newChapters = listOf("1.mp3", "2.mp3").map {
      ChapterId(documentUri(tree = "primary:Audiobooks/Book", document = "primary:Audiobooks/Book/$it"))
    }
    val new = bookContent(
      id = BookId(documentUri(tree = "primary:Audiobooks/Book", document = "primary:Audiobooks/Book")),
      chapters = newChapters,
    )

    val carried = carryOver.carryOver(new, PreviousBooks(listOf(old), scanned = setOf(new.id)))

    assertEquals(
      expected = new.copy(
        name = "Renamed",
        cover = File("cover.webp"),
        currentChapter = newChapters[1],
        positionInChapter = 42,
        lastPlayedAt = Instant.parse("2026-01-01T10:00:00Z"),
      ),
      actual = carried,
    )
    assertEquals(expected = listOf(bookmark.copy(bookId = new.id, chapterId = newChapters[1])), actual = bookmarkRepo.all)
    assertEquals(expected = new.id, actual = currentBookStore.data.first())
  }

  @Test
  fun `a book split out of a bigger one keeps the name from its files`() = runTest {
    val chapters = listOf("Book/1.mp3", "Other/1.mp3").map {
      ChapterId(documentUri(tree = "primary:Audiobooks", document = "primary:Audiobooks/$it"))
    }
    val old = bookContent(
      id = BookId(documentUri(tree = "primary:Audiobooks", document = "primary:Audiobooks")),
      chapters = chapters,
    ).copy(name = "Everything", lastPlayedAt = Instant.parse("2026-01-01T10:00:00Z"))
    val new = bookContent(
      id = BookId(documentUri(tree = "primary:Audiobooks", document = "primary:Audiobooks/Book")),
      chapters = chapters.take(1),
    )

    val carried = carryOver.carryOver(new, PreviousBooks(listOf(old), scanned = setOf(new.id)))

    assertEquals(expected = new.copy(lastPlayedAt = old.lastPlayedAt), actual = carried)
  }

  private fun documentUri(
    tree: String,
    document: String,
  ) = DocumentsContract.buildDocumentUriUsingTree(
    DocumentsContract.buildTreeDocumentUri("com.android.externalstorage.documents", tree),
    document,
  )

  private fun bookContent(
    id: BookId,
    chapters: List<ChapterId>,
  ) = BookContent(
    id = id,
    playbackSpeed = 1F,
    skipSilence = false,
    isActive = true,
    lastPlayedAt = Instant.EPOCH,
    author = null,
    name = "Book",
    addedAt = Instant.parse("2026-02-01T10:00:00Z"),
    chapters = chapters,
    currentChapter = chapters.first(),
    positionInChapter = 0,
    cover = null,
    gain = 0F,
    genre = null,
    narrator = null,
    series = null,
    part = null,
  )
}
