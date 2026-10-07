package voice.features.bookmark

import voice.core.data.Bookmark
import voice.core.data.Chapter
import voice.core.ui.formatTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class BookmarkListItemsTest {

  private val arrival = testChapter(name = "01 - Arrival")
  private val signal = testChapter(name = "02 - The Signal")
  private val static = testChapter(name = "03 - Static")
  private val chapters = listOf(arrival, signal, static)

  private fun items(
    bookmarks: List<Bookmark>,
    sort: BookmarkSort = BookmarkSort.Story,
    chapters: List<Chapter> = this.chapters,
    currentChapter: Chapter = signal,
    position: Duration = 5.minutes,
  ): List<String> {
    val book = testBook(chapters, currentChapter = currentChapter.id, positionInChapter = position)
    return bookmarkListItems(BookIndex(book), bookmarks, sort, now = today(10), zone = testZone).map { item ->
      when (item) {
        is BookmarkListItem.ChapterHeader -> "Chapter ${item.number} ${item.name}"
        is BookmarkListItem.Row -> {
          val chapter = if (item.bookmark.showChapter) " in ${item.bookmark.chapterNumber}" else ""
          "${item.bookmark.note}$chapter ${item.group.index}/${item.group.count}"
        }
        is BookmarkListItem.YouAreHere -> "Here ${item.time} ${item.percent}% ${item.group.index}/${item.group.count}"
      }
    }
  }

  @Test
  fun `story order groups by chapter around you are here`() {
    val bookmarks = listOf(
      testBookmark(static, 3.minutes, title = "Mira"),
      testBookmark(signal, 8.minutes, title = "Static"),
      testBookmark(arrival, 2.minutes, title = "Dock"),
      testBookmark(signal, 1.minutes, title = "Signal"),
    )
    assertEquals(
      expected = listOf(
        "Chapter 1 Arrival",
        "Dock 0/1",
        "Chapter 2 The Signal",
        "Signal 0/3",
        "Here ${chapterTime(5.minutes)} 50% 1/3",
        "Static 2/3",
        "Chapter 3 Static",
        "Mira 0/1",
      ),
      actual = items(bookmarks),
    )
  }

  @Test
  fun `you are here gets its own chapter when it has no bookmarks`() {
    val bookmarks = listOf(testBookmark(arrival, 2.minutes, title = "Dock"))
    assertEquals(
      expected = listOf(
        "Chapter 1 Arrival",
        "Dock 0/1",
        "Chapter 3 Static",
        "Here ${chapterTime(Duration.ZERO)} 66% 0/1",
      ),
      actual = items(bookmarks, currentChapter = static, position = Duration.ZERO),
    )
  }

  @Test
  fun `a book with a single chapter has no chapter headers`() {
    val single = testChapter(duration = 60.minutes, name = "Echoes of Tomorrow")
    val bookmarks = listOf(testBookmark(single, 40.minutes, title = "Later"), testBookmark(single, 10.minutes, title = "Early"))
    assertEquals(
      expected = listOf(
        "Early 0/3",
        "Here ${chapterTime(30.minutes, chapterDuration = 60.minutes)} 50% 1/3",
        "Later 2/3",
      ),
      actual = items(bookmarks, chapters = listOf(single), currentChapter = single, position = 30.minutes),
    )
  }

  @Test
  fun `recent puts the newest first and names the chapter`() {
    val bookmarks = listOf(
      testBookmark(arrival, 2.minutes, title = "Old", addedAt = today(7)),
      testBookmark(static, 3.minutes, title = "New", addedAt = today(9)),
      testBookmark(signal, 1.minutes, title = "Middle", addedAt = today(8)),
    )
    assertEquals(
      expected = listOf("New in 3 0/3", "Middle in 2 1/3", "Old in 1 2/3"),
      actual = items(bookmarks, sort = BookmarkSort.Recent),
    )
  }

  @Test
  fun `chapter marks count as chapters`() {
    val marked = testChapter(
      duration = 30.minutes,
      marks = listOf(Duration.ZERO to "Prologue", 10.minutes to "The Signal", 20.minutes to "Static"),
    )
    val bookmarks = listOf(testBookmark(marked, 25.minutes, title = "Mira"))
    assertEquals(
      expected = listOf(
        "Chapter 1 Prologue",
        "Here ${chapterTime(5.minutes)} 16% 0/1",
        "Chapter 3 Static",
        "Mira 0/1",
      ),
      actual = items(bookmarks, chapters = listOf(marked), currentChapter = marked, position = 5.minutes),
    )
  }

  @Test
  fun `the book bar pins every bookmark in book order`() {
    val book = testBook(chapters, currentChapter = signal.id, positionInChapter = 5.minutes)
    val bookmarks = listOf(
      testBookmark(static, 3.minutes, setBySleepTimer = true),
      testBookmark(arrival, 30.seconds, kind = Bookmark.Kind.Quote),
    )
    val bar = bookBar(BookIndex(book), bookmarks, playbackSpeed = 2F)!!
    assertEquals(expected = 1, actual = bar.currentSegment)
    assertEquals(expected = 0.5F, actual = bar.progress)
    assertEquals(expected = formatTime(7.5.minutes.inWholeMilliseconds), actual = bar.remaining)
    assertEquals(expected = listOf(Bookmark.Kind.Quote, Bookmark.Kind.Note), actual = bar.pins.map { it.kind })
    assertEquals(expected = listOf(false, true), actual = bar.pins.map { it.setBySleepTimer })
  }
}
