package voice.core.playback.session

import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.Instant
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.runner.RunWith
import voice.core.data.Book
import voice.core.data.BookCategory
import voice.core.data.Chapter
import voice.core.data.ChapterId
import voice.core.playback.session.search.book

@RunWith(AndroidJUnit4::class)
class LibraryChangeNotifierTest {

  private val scope = TestScope()
  private val book =
    book(
      chapters =
        listOf(
          Chapter(
            id = ChapterId("chapter"),
            name = "Chapter",
            duration = 60_000,
            fileLastModified = Instant.EPOCH,
            markData = emptyList(),
            fileSize = 0,
          ),
        ),
    )
  private val books = MutableStateFlow(listOf(book))
  private val session = mockk<MediaLibrarySession>(relaxed = true)
  private val notifier =
    LibraryChangeNotifier(
      bookRepository = mockk { every { flow() } returns books },
      mediaItemProvider =
        MediaItemProvider(
          bookRepository = mockk(),
          application = ApplicationProvider.getApplicationContext(),
          chapterRepo = mockk(),
          imageFileProvider = mockk(),
          currentBookStoreId = mockk(),
        ),
    )

  @Test
  fun `a book that moves to another group refreshes the lists that changed`() = scope.runTest {
    backgroundScope.launch { notifier.notifyChanges(session) }
    runCurrent()

    books.value = listOf(book.at(position = 30_000))
    advanceTimeBy(10.seconds)
    runCurrent()

    verify {
      session.notifyChildrenChanged(parentId(MediaId.Root), 1, null)
      session.notifyChildrenChanged(parentId(MediaId.Category(BookCategory.NOT_STARTED)), 0, null)
      session.notifyChildrenChanged(parentId(MediaId.Category(BookCategory.CURRENT)), 1, null)
    }
    verify(exactly = 0) {
      session.notifyChildrenChanged(
        parentId(MediaId.Category(BookCategory.FINISHED)),
        any(),
        any(),
      )
    }
  }

  @Test
  fun `while a book plays, its list is refreshed every 10 seconds`() = scope.runTest {
    books.value = listOf(book.at(position = 10_000))
    backgroundScope.launch { notifier.notifyChanges(session) }
    runCurrent()

    repeat(20) {
      books.value = listOf(book.at(position = 10_000L + it * 400))
      advanceTimeBy(400.milliseconds)
    }
    verify(exactly = 0) { session.notifyChildrenChanged(any(), any(), any()) }

    advanceTimeBy(2.seconds)
    runCurrent()
    verify(exactly = 1) { session.notifyChildrenChanged(any(), any(), any()) }
  }

  private fun Book.at(position: Long): Book =
    copy(content = content.copy(positionInChapter = position))

  private fun parentId(mediaId: MediaId): String =
    Json.encodeToString(MediaId.serializer(), mediaId)
}
