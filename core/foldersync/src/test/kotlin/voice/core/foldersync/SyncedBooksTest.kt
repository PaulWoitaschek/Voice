package voice.core.foldersync

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.runner.RunWith
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class SyncedBooksTest {

  @Test
  fun `the place of the newest copy is written with its speed`() {
    val older = book(path = "Book", currentFile = 0, position = 5, lastPlayedAt = 1000)
    val newer = book(path = "Copy of Book", currentFile = 1, position = 7, lastPlayedAt = 2000, playbackSpeed = 1.5F)

    assertEquals(
      expected = SyncedPlace(file = 1, position = 7, speed = 1.5F, playedAt = 2000),
      actual = listOf(older, newer).toSyncedBook(own = null, ownFileExists = true, now = NOW).place,
    )
  }

  @Test
  fun `a book never played and never renamed has nothing to say`() {
    assertEquals(SyncedBook(), listOf(book()).toSyncedBook(own = null, ownFileExists = true, now = NOW))
  }

  @Test
  fun `a name given before this device synced the folder counts as old`() {
    val book = book(customName = "Mine")

    assertEquals(SyncedText("Mine", changedAt = 0), listOf(book).toSyncedBook(own = null, ownFileExists = false, now = NOW).name)
    assertEquals(SyncedText("Mine", changedAt = NOW), listOf(book).toSyncedBook(own = null, ownFileExists = true, now = NOW).name)
  }

  @Test
  fun `a changed name is changed now, an unchanged one keeps its time`() {
    val own = SyncedBook(name = SyncedText("Old", changedAt = 1000))

    assertEquals(
      SyncedText("New", changedAt = NOW),
      listOf(book(customName = "New")).toSyncedBook(own, ownFileExists = true, now = NOW).name,
    )
    assertEquals(own.name, listOf(book(customName = "Old")).toSyncedBook(own, ownFileExists = true, now = NOW).name)
  }

  @Test
  fun `a name that is the files' name here is unchanged`() {
    // another device's folder is called differently, and a name equal to the files' name isn't kept as a rename
    val own = SyncedBook(name = SyncedText(value = "Book", changedAt = 1000))

    assertEquals(own.name, listOf(book(path = "Book")).toSyncedBook(own, ownFileExists = true, now = NOW).name)
  }

  @Test
  fun `going back to the files' name is a change`() {
    val own = SyncedBook(name = SyncedText("Mine", changedAt = 1000))

    assertEquals(SyncedText(value = null, changedAt = NOW), listOf(book()).toSyncedBook(own, ownFileExists = true, now = NOW).name)
  }

  @Test
  fun `what changed later wins, and on a tie this device's stays`() {
    val mine = SyncedBook(place = place(playedAt = 2000), name = SyncedText("Mine", changedAt = 1000))
    val other = SyncedBook(place = place(file = 0, playedAt = 1000), name = SyncedText("Other", changedAt = 3000))

    assertEquals(SyncedBook(place = mine.place, name = other.name), mine.with(other))
    assertEquals(mine, mine.with(mine.copy(name = SyncedText("Other", changedAt = 1000))))
  }

  @Test
  fun `a device that didn't play or rename the book takes nothing away`() {
    val mine = SyncedBook(place = place(), name = SyncedText("Mine", changedAt = 0))

    assertEquals(mine, mine.with(SyncedBook()))
    assertEquals(mine, SyncedBook().with(mine))
  }
}

private const val NOW = 5000L
