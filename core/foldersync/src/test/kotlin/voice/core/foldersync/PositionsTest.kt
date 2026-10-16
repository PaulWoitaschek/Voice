package voice.core.foldersync

import android.provider.DocumentsContract
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.runner.RunWith
import voice.core.data.Book
import voice.core.data.BookId
import voice.core.data.Bookmark
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.uuid.Uuid

@RunWith(AndroidJUnit4::class)
class PositionsTest {

  @Test
  fun `only books in the folder are written`() {
    val inFolder = book(lastPlayedAt = 1)
    val otherFolder = book(tree = "primary:Other", lastPlayedAt = 1)
    val singleFileUri = DocumentsContract.buildDocumentUri("com.android.externalstorage.documents", "primary:Book.m4b")
    val singleFile = book(lastPlayedAt = 1).let { Book(it.content.copy(id = BookId(singleFileUri.toString())), it.chapters) }

    val books = listOf(inFolder, otherFolder, singleFile)

    assertEquals(expected = listOf(inFolder), actual = books.keyedIn(folder()).map { it.book })
    assertEquals(expected = emptyList(), actual = books.keyedIn(singleFileUri))
  }

  @Test
  fun `a newer position is taken over`() {
    val book = book(currentFile = 0, position = 5000, lastPlayedAt = 1000)

    assertEquals(
      expected = TakenOverPosition(book.chapters[1].id, positionInChapter = 3000, playedAt = Instant.ofEpochMilli(2000)),
      actual = book.positionFrom(place(file = 1, position = 3000, playedAt = 2000)),
    )
  }

  @Test
  fun `a position that isn't newer stays`() {
    val book = book(currentFile = 0, position = 5000, lastPlayedAt = 1000)

    assertNull(book.positionFrom(place(file = 1, position = 3000, playedAt = 1000)))
    assertNull(book.positionFrom(place(file = 1, position = 3000, playedAt = 999)))
  }

  @Test
  fun `a newer time at the same place is not taken over`() {
    // taking over a place seeks, which stores a newer time at that place, and the other device must not take it back
    val book = book(currentFile = 1, position = 5000, lastPlayedAt = 1000)

    assertNull(book.positionFrom(place(file = 1, position = 6000, playedAt = 2000)))
  }

  @Test
  fun `a position past the end of its file stops at the end`() {
    val book = book(lastPlayedAt = 1000)

    assertEquals(60_000, book.positionFrom(place(file = 1, position = 90_000, playedAt = 2000))?.positionInChapter)
  }

  @Test
  fun `a file the book doesn't have is ignored`() {
    val book = book(lastPlayedAt = 1000)

    assertNull(book.positionFrom(place(file = 2, position = 0, playedAt = 2000)))
  }

  @Test
  fun `a file with unknown fields can be read, a broken one is skipped`() {
    val text = """{"books":{"a":{"place":{"file":1,"position":2,"speed":1.5,"playedAt":3},"author":{}}},"other":{}}"""
    val file = SyncFile(mapOf("a" to SyncedBook(place = SyncedPlace(file = 1, position = 2, speed = 1.5F, playedAt = 3))))

    assertEquals(file, parseSyncFile(text))
    assertEquals(file, parseSyncFile(file.encode()))
    assertNull(parseSyncFile("""{"books":{"a":{"place":{"file":1"""))
    assertNull(parseSyncFile("garbage"))
  }

  @Test
  fun `a bookmark kind this version doesn't know is read as a note`() {
    val id = "00000000-0000-0000-0000-000000000001"
    val text = """{"books":{"a":{"bookmarks":{"$id":{"file":1,"time":2,"kind":"Later","addedAt":3,"changedAt":4}}}}}"""

    assertEquals(
      expected = SyncedBookmark(file = 1, time = 2, kind = Bookmark.Kind.Note, addedAt = 3, changedAt = 4),
      actual = parseSyncFile(text)?.books?.get("a")?.bookmarks?.get(Uuid.parse(id)),
    )
  }
}
