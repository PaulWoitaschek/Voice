package voice.core.playback.session

import androidx.media3.session.MediaConstants.EXTRAS_KEY_COMPLETION_PERCENTAGE
import androidx.media3.session.MediaConstants.EXTRAS_KEY_COMPLETION_STATUS
import androidx.media3.session.MediaConstants.EXTRAS_VALUE_COMPLETION_STATUS_FULLY_PLAYED
import androidx.media3.session.MediaConstants.EXTRAS_VALUE_COMPLETION_STATUS_NOT_PLAYED
import androidx.media3.session.MediaConstants.EXTRAS_VALUE_COMPLETION_STATUS_PARTIALLY_PLAYED
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.coEvery
import io.mockk.mockk
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.runner.RunWith
import voice.core.data.Book
import voice.core.data.BookCategory
import voice.core.data.Chapter
import voice.core.data.ChapterId
import voice.core.data.repo.BookRepository
import voice.core.playback.session.search.book

@RunWith(AndroidJUnit4::class)
class MediaItemProviderTest {

  private val books = mutableListOf<Book>()
  private val provider =
    MediaItemProvider(
      bookRepository =
        mockk<BookRepository> {
          coEvery { all() } answers { books }
          coEvery { this@mockk.get(any()) } answers { books.find { it.id == firstArg() } }
        },
      application = ApplicationProvider.getApplicationContext(),
      chapterRepo = mockk(),
      imageFileProvider = mockk(),
      currentBookStoreId = mockk(),
    )

  @Test
  fun `the library is split into the groups of the app, leaving out empty ones`() = runTest {
    books += libraryBook("Current", position = 30_000)
    books += libraryBook("Finished", position = 60_000)
    books += libraryBook("Hidden", position = 0, hidden = true)

    assertEquals(
      expected =
        listOf(MediaId.Category(BookCategory.CURRENT), MediaId.Category(BookCategory.FINISHED)),
      actual = children(MediaId.Root).map { it.mediaId.toMediaIdOrNull() },
    )
  }

  @Test
  fun `each group is sorted like in the app and leaves out hidden books`() = runTest {
    books += libraryBook("B", position = 0)
    books += libraryBook("A", position = 0)
    books += libraryBook("Hidden", position = 0, hidden = true)
    books += libraryBook("Old", position = 30_000, lastPlayedAt = Instant.ofEpochSecond(1))
    books += libraryBook("New", position = 30_000, lastPlayedAt = Instant.ofEpochSecond(2))

    assertEquals(
      expected = listOf("A", "B"),
      actual = titles(BookCategory.NOT_STARTED),
    )
    assertEquals(
      expected = listOf("New", "Old"),
      actual = titles(BookCategory.CURRENT),
    )
  }

  @Test
  fun `a book shows its author and how far it was listened to`() {
    val metadata = provider.mediaItem(libraryBook("Current", position = 15_000)).mediaMetadata

    assertEquals(expected = "Author", actual = metadata.artist)
    assertEquals(expected = "Author · 00:30 left", actual = metadata.subtitle?.toString())
    assertEquals(
      expected = EXTRAS_VALUE_COMPLETION_STATUS_PARTIALLY_PLAYED,
      actual = metadata.extras!!.getInt(EXTRAS_KEY_COMPLETION_STATUS),
    )
    assertEquals(
      expected = 0.25,
      actual = metadata.extras!!.getDouble(EXTRAS_KEY_COMPLETION_PERCENTAGE),
    )
  }

  @Test
  fun `books not started and finished show only their author`() {
    val subtitles =
      listOf(0L, 60_000L).map { position ->
        provider.mediaItem(libraryBook("Book", position)).mediaMetadata.subtitle
      }

    assertEquals(expected = listOf(null, null), actual = subtitles)
  }

  @Test
  fun `books not started and finished get their badge`() {
    assertEquals(
      expected =
        listOf(
          EXTRAS_VALUE_COMPLETION_STATUS_NOT_PLAYED,
          EXTRAS_VALUE_COMPLETION_STATUS_FULLY_PLAYED,
        ),
      actual =
        listOf(0L, 60_000L).map { position ->
          provider
            .mediaItem(libraryBook("Book", position))
            .mediaMetadata
            .extras!!
            .getInt(EXTRAS_KEY_COMPLETION_STATUS)
        },
    )
  }

  @Test
  fun `a chapter picked in the car plays the whole book from that chapter`() = runTest {
    books += book(twoChapters())
    val marks = provider.playbackItems(books.single())

    val items = provider.mediaItemsWithStartPosition(marks[1].mediaId)!!

    assertEquals(expected = marks.map { it.mediaId }, actual = items.mediaItems.map { it.mediaId })
    assertEquals(expected = 1, actual = items.startIndex)
    assertEquals(expected = 0, actual = items.startPositionMs)
  }

  @Test
  fun `a chapter mark whose end moved still starts at that mark`() = runTest {
    books += book(twoChapters())
    val mark = secondMark()

    val items =
      provider.mediaItemsWithStartPosition(mark.copy(endMs = mark.endMs - 1_000).string())!!

    assertEquals(expected = 1, actual = items.startIndex)
    assertEquals(expected = 0, actual = items.startPositionMs)
  }

  @Test
  fun `a chapter mark that is gone starts where the book stopped`() = runTest {
    books += book(twoChapters(), positionInChapter = 30_000)

    val items = provider.mediaItemsWithStartPosition(secondMark().copy(markIndex = 1).string())!!

    assertEquals(expected = 0, actual = items.startIndex)
    assertEquals(expected = 30_000, actual = items.startPositionMs)
  }

  private fun twoChapters(): List<Chapter> =
    listOf("First", "Second").map {
      Chapter(
        id = ChapterId(it),
        name = it,
        duration = 60_000,
        fileLastModified = Instant.EPOCH,
        markData = emptyList(),
        fileSize = 0,
      )
    }

  private fun secondMark(): MediaId.ChapterMark =
    assertIs(provider.playbackItems(books.single())[1].mediaId.toMediaIdOrNull())

  private fun MediaId.string(): String = Json.encodeToString(MediaId.serializer(), this)

  private suspend fun titles(category: BookCategory): List<String> =
    children(MediaId.Category(category)).map { it.mediaMetadata.title.toString() }

  private suspend fun children(mediaId: MediaId) = provider.children(mediaId.string())!!

  private fun libraryBook(
    name: String,
    position: Long,
    lastPlayedAt: Instant = Instant.EPOCH,
    hidden: Boolean = false,
  ): Book {
    val chapter =
      Chapter(
        id = ChapterId(name),
        name = name,
        duration = 60_000,
        fileLastModified = Instant.EPOCH,
        markData = emptyList(),
        fileSize = 0,
      )
    val book = book(chapters = listOf(chapter), positionInChapter = position)
    return book.copy(
      content =
        book.content.copy(
          lastPlayedAt = lastPlayedAt,
          playbackSpeed = 1.5F,
          metadata = book.content.metadata.copy(name = name, author = "Author"),
        ),
      custom = book.custom.copy(hidden = hidden),
    )
  }
}
