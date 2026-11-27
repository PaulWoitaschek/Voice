package voice.core.playback.playstate

import androidx.media3.common.Player
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.coEvery
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import voice.core.data.BookContent
import voice.core.data.Chapter
import voice.core.data.ChapterId
import voice.core.data.repo.BookRepository
import voice.core.playback.FakePlayer
import voice.core.playback.session.search.book

@RunWith(AndroidJUnit4::class)
class PositionUpdaterTest {

  private val scope = TestScope()
  private var book =
    book(
      listOf(
        Chapter(
          id = ChapterId("chapter"),
          name = "Chapter",
          duration = 100_000,
          fileLastModified = Instant.fromEpochMilliseconds(0),
          markData = emptyList(),
          fileSize = 0,
        ),
      ),
    )
  private val bookRepository =
    mockk<BookRepository> {
      coEvery { updateBook(book.id, any()) } answers
        {
          book = book.update(secondArg<(BookContent) -> BookContent>())
        }
    }
  private val playStateManager = PlayStateManager()
  private val player = FakePlayer(book.id, contentPositionMs = 5_000)

  init {
    PositionUpdater(bookRepository, scope.backgroundScope, playStateManager).attachTo(player)
  }

  @Test
  fun `while a book plays, the position is saved every 30 seconds`() = scope.runTest {
    playStateManager.playState = PlayStateManager.PlayState.Playing(book.id)

    advanceTimeBy(29.seconds)
    runCurrent()
    assertEquals(expected = 0, actual = book.content.positionInChapter)

    advanceTimeBy(1.seconds)
    runCurrent()
    assertEquals(expected = 5_000, actual = book.content.positionInChapter)
  }

  @Test
  fun `a pause saves the position at once`() = scope.runTest {
    player.update {
      it.setPlayWhenReady(false, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
    }
    runCurrent()

    assertEquals(expected = 5_000, actual = book.content.positionInChapter)
  }
}
