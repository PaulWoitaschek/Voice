package voice.features.widget

import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import voice.core.data.Book
import voice.core.data.BookId
import voice.core.data.store.CurrentBookStore
import voice.core.playback.LastPlaybackSpeedStore
import voice.core.playback.playstate.PlayStateManager
import voice.core.sleeptimer.SleepTimerState
import voice.core.ui.CoverColorsStore
import voice.core.ui.ThemeColorScheme
import voice.core.ui.ThemeColorSchemeStore

class WidgetDataTest {

  private val bookId = BookId("book")
  private val chapter = chapter(duration = 600_000)
  private val currentBook = MutableStateFlow<Book?>(null)

  // the end and 10 seconds before it are in the same minute, which is all the widget shows
  private val tenSecondsBeforeTheEnd = 589_999L
  private val theEnd = 599_999L

  @Test
  fun `paused, skip forward on the widget to the end, the widget shows finished`() = runTest {
    currentBook.value = book(positionInChapter = tenSecondsBeforeTheEnd)

    widgetData().nowPlaying.test {
      assertFalse(awaitItem().book!!.finished)

      currentBook.value = book(positionInChapter = theEnd)

      assertTrue(awaitItem().book!!.finished)
    }
  }

  @Test
  fun `at the end, skip back, the widget no longer shows finished`() = runTest {
    currentBook.value = book(positionInChapter = theEnd)

    widgetData().nowPlaying.test {
      assertTrue(awaitItem().book!!.finished)

      currentBook.value = book(positionInChapter = tenSecondsBeforeTheEnd)

      assertFalse(awaitItem().book!!.finished)
    }
  }

  private fun book(positionInChapter: Long): Book =
    book(chapters = listOf(chapter), positionInChapter = positionInChapter, id = bookId)

  private fun TestScope.widgetData() =
    WidgetData(
      bookRepository = mockk { every { flow(bookId) } returns currentBook },
      // only the shelf reads them
      bookContentRepo = mockk(relaxed = true),
      customMetadataRepo = mockk(relaxed = true),
      currentBookResolver = lazy { mockk() },
      currentBookStore = CurrentBookStore(MemoryDataStore(bookId)),
      lastPlaybackSpeedStore = LastPlaybackSpeedStore(MemoryDataStore(1F)),
      playStateManager = PlayStateManager(),
      sleepTimer = mockk { every { state } returns MutableStateFlow(SleepTimerState.Disabled) },
      themeColorSchemeStore =
        ThemeColorSchemeStore(MemoryDataStore(ThemeColorScheme.entries.first())),
      coverColorsStore = CoverColorsStore(MemoryDataStore(false)),
      coverSeedColors = mockk(),
      images = mockk(),
      scope = backgroundScope,
    )
}
