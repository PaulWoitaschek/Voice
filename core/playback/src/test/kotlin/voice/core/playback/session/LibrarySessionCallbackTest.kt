package voice.core.playback.session

import android.os.Bundle
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import voice.core.data.Book
import voice.core.data.BookId
import voice.core.data.Chapter
import voice.core.data.ChapterId
import voice.core.data.repo.BookRepository
import voice.core.data.repo.BookmarkRepo
import voice.core.data.sleeptimer.SleepTimerPreference
import voice.core.playback.MemoryDataStore
import voice.core.playback.player.VoicePlayer
import voice.core.playback.session.search.BookSearchHandler
import voice.core.playback.session.search.BookSearchParser
import voice.core.playback.session.search.book
import voice.core.sleeptimer.SleepTimer
import voice.core.sleeptimer.SleepTimerMode
import voice.core.sleeptimer.SleepTimerState
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes
import kotlin.uuid.Uuid

@RunWith(AndroidJUnit4::class)
class LibrarySessionCallbackTest {

  private val sleepTimer = FakeSleepTimer()
  private val sleepTimerPreferenceStore = MemoryDataStore(
    SleepTimerPreference.Default.copy(duration = 15.minutes),
  )
  private val player = mockk<VoicePlayer>(relaxed = true)
  private val bookRepository = mockk<BookRepository>(relaxed = true)
  private val bookmarkRepo = mockk<BookmarkRepo>(relaxed = true)
  private var currentBook: Book? = null

  private fun callback(scope: TestScope) = LibrarySessionCallback(
    mediaItemProvider = mockk(relaxed = true),
    scope = scope,
    player = player,
    bookSearchParser = mockk<BookSearchParser>(relaxed = true),
    bookSearchHandler = mockk<BookSearchHandler>(relaxed = true),
    currentBookStoreId = MemoryDataStore(currentBook?.content?.id),
    bookRepository = bookRepository,
    bookmarkRepo = bookmarkRepo,
    sleepTimer = sleepTimer,
    sleepTimerPreferenceStore = sleepTimerPreferenceStore,
  )

  private fun sendCommand(
    scope: TestScope,
    action: String,
  ) {
    callback(scope).onCustomCommand(
      mockk<MediaSession>(relaxed = true),
      mockk<MediaSession.ControllerInfo>(relaxed = true),
      SessionCommand(action, Bundle.EMPTY),
      Bundle.EMPTY,
    )
  }

  private fun chapter() = Chapter(
    id = ChapterId(Uuid.random().toString()),
    name = "chapter",
    duration = 10_000,
    fileLastModified = Instant.EPOCH,
    markData = emptyList(),
    fileSize = 0,
  )

  private fun setCurrentBook(book: Book) {
    currentBook = book
    coEvery { bookRepository.get(book.content.id) } returns book
  }

  @Test
  fun `sleep timer action enables the timer with the stored duration when disabled`() = runTest {
    sendCommand(this, LibrarySessionCallback.ACTION_SLEEP_TIMER)
    advanceUntilIdle()

    assertEquals(
      expected = SleepTimerState.Enabled.WithDuration(15.minutes),
      actual = sleepTimer.state.value,
    )
  }

  @Test
  fun `sleep timer action disables the timer when enabled`() = runTest {
    sleepTimer.enable(SleepTimerMode.TimedWithDuration(15.minutes))
    sendCommand(this, LibrarySessionCallback.ACTION_SLEEP_TIMER)
    advanceUntilIdle()

    assertEquals(expected = SleepTimerState.Disabled, actual = sleepTimer.state.value)
  }

  @Test
  fun `next chapter action force seeks to next`() = runTest {
    sendCommand(this, LibrarySessionCallback.ACTION_NEXT_CHAPTER)
    advanceUntilIdle()

    verify { player.forceSeekToNext() }
  }

  @Test
  fun `previous chapter action force seeks to previous`() = runTest {
    sendCommand(this, LibrarySessionCallback.ACTION_PREVIOUS_CHAPTER)
    advanceUntilIdle()

    verify { player.forceSeekToPrevious() }
  }

  @Test
  fun `skip silence action enables skip silence when the current book has it disabled`() = runTest {
    setCurrentBook(book(listOf(chapter())))
    sendCommand(this, LibrarySessionCallback.ACTION_SKIP_SILENCE)
    advanceUntilIdle()

    verify { player.setSkipSilenceEnabled(true) }
  }

  @Test
  fun `bookmark action adds a bookmark at the current book position`() = runTest {
    val book = book(listOf(chapter()))
    setCurrentBook(book)
    sendCommand(this, LibrarySessionCallback.ACTION_BOOKMARK)
    advanceUntilIdle()

    coVerify {
      bookmarkRepo.addBookmarkAtBookPosition(book = book, title = null, setBySleepTimer = false)
    }
  }

  private class FakeSleepTimer : SleepTimer {
    override val state: StateFlow<SleepTimerState>
      get() = stateFlow

    private val stateFlow = MutableStateFlow<SleepTimerState>(SleepTimerState.Disabled)

    override fun enable(mode: SleepTimerMode) {
      stateFlow.value = when (mode) {
        is SleepTimerMode.TimedWithDuration -> SleepTimerState.Enabled.WithDuration(mode.duration)
        SleepTimerMode.TimedWithDefault -> error("TimedWithDefault is not used in these tests")
        SleepTimerMode.EndOfChapter -> SleepTimerState.Enabled.WithEndOfChapter
      }
    }

    override fun disable() {
      stateFlow.value = SleepTimerState.Disabled
    }
  }
}
