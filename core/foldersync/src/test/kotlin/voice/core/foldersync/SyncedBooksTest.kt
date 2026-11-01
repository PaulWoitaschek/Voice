package voice.core.foldersync

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.runner.RunWith
import voice.core.data.Book
import voice.core.data.Bookmark

@RunWith(AndroidJUnit4::class)
class SyncedBooksTest {

  @Test
  fun `the place of the newest copy is written with its speed`() {
    val older = book(path = "Book", currentFile = 0, position = 5, lastPlayedAt = 1000)
    val newer = book(path = "Copy of Book", currentFile = 1, position = 7, lastPlayedAt = 2000, playbackSpeed = 1.5F)

    assertEquals(
      expected = SyncedPlace(file = 1, position = 7, speed = 1.5F, playedAt = 2000),
      actual = listOf(older, newer).synced().place,
    )
  }

  @Test
  fun `a book never played, renamed or bookmarked has nothing to say`() {
    assertEquals(SyncedBook(), listOf(book()).synced())
  }

  @Test
  fun `a name given before this device synced the folder counts as old`() {
    val book = book(customName = "Mine")

    assertEquals(SyncedText("Mine", changedAt = 0), listOf(book).synced(ownFileExists = false).name)
    assertEquals(SyncedText("Mine", changedAt = NOW), listOf(book).synced(ownFileExists = true).name)
  }

  @Test
  fun `a changed name is changed now, an unchanged one keeps its time`() {
    val own = SyncedBook(name = SyncedText("Old", changedAt = 1000))

    assertEquals(SyncedText("New", changedAt = NOW), listOf(book(customName = "New")).synced(own).name)
    assertEquals(own.name, listOf(book(customName = "Old")).synced(own).name)
  }

  @Test
  fun `a name that is the files' name here is unchanged`() {
    // another device's folder is called differently, and a name equal to the files' name isn't kept as a rename
    val own = SyncedBook(name = SyncedText(value = "Book", changedAt = 1000))

    assertEquals(own.name, listOf(book(path = "Book")).synced(own).name)
  }

  @Test
  fun `going back to the files' name is a change`() {
    val own = SyncedBook(name = SyncedText("Mine", changedAt = 1000))

    assertEquals(SyncedText(value = null, changedAt = NOW), listOf(book()).synced(own).name)
  }

  @Test
  fun `a new bookmark is written with the time it was added`() {
    val book = book()
    val bookmark = book.bookmark(addedAt = 100)

    assertEquals(
      expected = mapOf(bookmark.id.value to bookmark.synced(changedAt = 100)),
      actual = listOf(book).synced(bookmarks = listOf(bookmark)).bookmarks,
    )
  }

  @Test
  fun `a changed bookmark is changed now, an unchanged one keeps its time`() {
    val book = book()
    val bookmark = book.bookmark(title = "Old")
    val own = SyncedBook(bookmarks = mapOf(bookmark.id.value to bookmark.synced(changedAt = 1000)))

    assertEquals(own.bookmarks, listOf(book).synced(own, bookmarks = listOf(bookmark)).bookmarks)
    val renamed = bookmark.copy(title = "New")
    assertEquals(
      expected = mapOf(bookmark.id.value to renamed.synced(changedAt = NOW)),
      actual = listOf(book).synced(own, bookmarks = listOf(renamed)).bookmarks,
    )
  }

  @Test
  fun `a deleted bookmark is written as deleted, and stays so`() {
    val bookmark = book().bookmark()
    val own = SyncedBook(bookmarks = mapOf(bookmark.id.value to bookmark.synced()))

    val deleted = listOf(book()).synced(own)

    assertEquals(SyncedBook(deletedBookmarks = mapOf(bookmark.id.value to NOW)), deleted)
    assertEquals(deleted, listOf(book()).synced(own = deleted))
  }

  @Test
  fun `a bookmark brought back after deleting it is changed now`() {
    val bookmark = book().bookmark()
    val own = SyncedBook(deletedBookmarks = mapOf(bookmark.id.value to 1000))

    assertEquals(
      expected = SyncedBook(bookmarks = mapOf(bookmark.id.value to bookmark.synced(changedAt = NOW))),
      actual = listOf(book()).synced(own, bookmarks = listOf(bookmark)),
    )
  }

  @Test
  fun `the bookmarks of every copy are written`() {
    val played = book(path = "Book", lastPlayedAt = 2000)
    val copy = book(path = "Copy of Book")
    val bookmark = copy.bookmark(file = 0)
    val own = SyncedBook(bookmarks = mapOf(bookmark.id.value to bookmark.synced(file = 0)))

    assertEquals(own.bookmarks, listOf(played, copy).synced(own, bookmarks = listOf(bookmark)).bookmarks)
  }

  @Test
  fun `an author is changed now when it's changed here or put back to the files' one`() {
    val own = SyncedBook(author = SyncedText("Old", changedAt = 1000))

    assertEquals(SyncedText("New", changedAt = NOW), listOf(book(customAuthor = "New")).synced(own).author)
    assertEquals(own.author, listOf(book(customAuthor = "Old")).synced(own).author)
    assertEquals(SyncedText(value = null, changedAt = NOW), listOf(book(author = "Files")).synced(own).author)
  }

  @Test
  fun `a series and part are changed now when they're changed here or put back to the files' ones`() {
    val own = SyncedBook(series = SyncedText("Old", changedAt = 1000), part = SyncedText("1", changedAt = 1000))

    val changed = listOf(book(customSeries = "New", customPart = "2")).synced(own)
    val unchanged = listOf(book(customSeries = "Old", customPart = "1")).synced(own)
    val putBack = listOf(book(series = "Files", part = "3")).synced(own)

    assertEquals(SyncedText("New", changedAt = NOW), changed.series)
    assertEquals(SyncedText("2", changedAt = NOW), changed.part)
    assertEquals(own.series, unchanged.series)
    assertEquals(own.part, unchanged.part)
    assertEquals(SyncedText(value = null, changedAt = NOW), putBack.series)
    assertEquals(SyncedText(value = null, changedAt = NOW), putBack.part)
  }

  @Test
  fun `a series and part like in the files aren't written`() {
    val files = book(series = "Files", part = "3")
    val synced = SyncedBook(series = SyncedText("Files", changedAt = 1000), part = SyncedText("3", changedAt = 1000))

    assertEquals(SyncedBook(), synced.withoutFilesNames(listOf(files)))
  }

  @Test
  fun `what changed later wins, and on a tie this device's stays`() {
    val mine =
      SyncedBook(
        place = place(playedAt = 2000),
        name = SyncedText("Mine", changedAt = 1000),
        author = SyncedText("Mine", changedAt = 3000),
        series = SyncedText("Mine", changedAt = 1000),
        part = SyncedText("1", changedAt = 3000),
      )
    val other =
      SyncedBook(
        place = place(file = 0, playedAt = 1000),
        name = SyncedText("Other", changedAt = 3000),
        author = SyncedText("Other", changedAt = 1000),
        series = SyncedText("Other", changedAt = 3000),
        part = SyncedText("2", changedAt = 1000),
      )

    assertEquals(
      expected =
        SyncedBook(
          place = mine.place,
          name = other.name,
          author = mine.author,
          series = other.series,
          part = mine.part,
        ),
      actual = mine.with(other),
    )
    assertEquals(mine, mine.with(mine.copy(name = SyncedText("Other", changedAt = 1000))))
  }

  @Test
  fun `a deletion wins over an older change, and a newer change brings a bookmark back`() {
    val bookmark = book().bookmark()
    val changed = SyncedBook(bookmarks = mapOf(bookmark.id.value to bookmark.synced(changedAt = 1000)))
    val deleted = SyncedBook(deletedBookmarks = mapOf(bookmark.id.value to 2000))
    val changedAgain = SyncedBook(bookmarks = mapOf(bookmark.id.value to bookmark.synced(changedAt = 3000)))

    assertEquals(deleted, changed.with(deleted))
    assertEquals(deleted, deleted.with(changed))
    assertEquals(changedAgain, deleted.with(changedAgain))
  }

  @Test
  fun `a device that didn't play, rename or bookmark the book takes nothing away`() {
    val bookmark = book().bookmark()
    val mine =
      SyncedBook(
        place = place(),
        name = SyncedText("Mine", changedAt = 0),
        bookmarks = mapOf(bookmark.id.value to bookmark.synced()),
      )

    assertEquals(mine, mine.with(SyncedBook()))
    assertEquals(mine, SyncedBook().with(mine))
  }

  private fun List<Book>.synced(
    own: SyncedBook? = null,
    ownFileExists: Boolean = true,
    bookmarks: List<Bookmark> = emptyList(),
  ): SyncedBook = toSyncedBook(bookmarks.groupBy { it.chapterId }, own, ownFileExists, NOW)
}

private const val NOW = 5000L
