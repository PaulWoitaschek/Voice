package voice.playbackScreen

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Test
import voice.common.BookId
import voice.common.DispatcherProvider
import voice.data.Book
import voice.data.BookContent
import voice.data.Bookmark
import voice.data.Chapter
import voice.data.ChapterId
import voice.pref.inmemory.InMemoryPref
import voice.sleepTimer.SleepTimer
import voice.sleepTimer.SleepTimerViewState
import java.time.Instant
import java.time.LocalTime
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

class BookPlayViewModelTest {

  private val scope = TestScope()
  private val sleepTimerPref = InMemoryPref(15)
  private val autoSleepTimerEnabledPref = InMemoryPref(false)
  private val autoSleepTimerStartTimePref = InMemoryPref(22)
  private val autoSleepTimerEndTimePref = InMemoryPref(6)
  private val autoSleepTimerDurationPref = InMemoryPref(30)
  private val book = book()
  private val sleepTimer = mockk<SleepTimer> {
    var sleepTimerActive = false
    every { sleepTimerActive() } answers { sleepTimerActive }
    coEvery { setActive(any<Duration>()) } answers {
      sleepTimerActive = true
    }
    coEvery { setActive(any<Boolean>()) } answers {
      sleepTimerActive = firstArg()
    }
  }
  private val viewModel = BookPlayViewModel(
    bookRepository = mockk {
      coEvery { get(book.id) } returns book
    },
    player = mockk(),
    sleepTimer = sleepTimer,
    playStateManager = mockk(),
    currentBookId = mockk(),
    navigator = mockk(),
    bookmarkRepository = mockk {
      coEvery { addBookmarkAtBookPosition(book, any(), any()) } returns Bookmark(
        bookId = book.id,
        chapterId = book.currentChapter.id,
        addedAt = Instant.now(),
        setBySleepTimer = true,
        id = Bookmark.Id(UUID.randomUUID()),
        time = 0L,
        title = null,
      )
    },
    volumeGainFormatter = mockk(),
    batteryOptimization = mockk(),
    sleepTimePref = sleepTimerPref,
    autoSleepTimerEnabledPref = autoSleepTimerEnabledPref,
    autoSleepTimerStartTimePref = autoSleepTimerStartTimePref,
    autoSleepTimerEndTimePref = autoSleepTimerEndTimePref,
    autoSleepTimerDurationPref = autoSleepTimerDurationPref,
    bookId = book.id,
    dispatcherProvider = DispatcherProvider(scope.coroutineContext, scope.coroutineContext),
  )

  @Test
  fun sleepTimerValueChanging() = scope.runTest {
    fun assertDialogSleepTime(expected: Int) {
      viewModel.dialogState.value shouldBe BookPlayDialogViewState.SleepTimer(SleepTimerViewState(expected))
    }

    viewModel.toggleSleepTimer()
    assertDialogSleepTime(15)

    fun incrementAndAssert(time: Int) {
      viewModel.incrementSleepTime()
      assertDialogSleepTime(time)
    }

    fun decrementAndAssert(time: Int) {
      viewModel.decrementSleepTime()
      assertDialogSleepTime(time)
    }

    decrementAndAssert(10)
    decrementAndAssert(5)
    decrementAndAssert(4)
    decrementAndAssert(3)
    decrementAndAssert(2)
    decrementAndAssert(1)

    decrementAndAssert(1)

    incrementAndAssert(2)
    incrementAndAssert(3)
    incrementAndAssert(4)
    incrementAndAssert(5)
    incrementAndAssert(10)
    incrementAndAssert(15)
  }

  @Test
  fun sleepTimerSettingFixedValue() = scope.runTest {
    viewModel.toggleSleepTimer()
    viewModel.onAcceptSleepTime(10)
    sleepTimerPref.value shouldBe 15
    verify(exactly = 1) {
      sleepTimer.setActive(10.minutes)
    }
  }

  @Test
  fun deactivateSleepTimer() {
    viewModel.toggleSleepTimer()
    viewModel.onAcceptSleepTime(10)
    viewModel.toggleSleepTimer()
    verifyOrder {
      sleepTimer.setActive(10.minutes)
      sleepTimer.setActive(false)
    }
    sleepTimer.sleepTimerActive() shouldBe false
  }

  @Test
  fun `auto sleep timer activates during configured time`() = scope.runTest {
    autoSleepTimerEnabledPref.value = true
    autoSleepTimerStartTimePref.value = 22
    autoSleepTimerEndTimePref.value = 6
    autoSleepTimerDurationPref.value = 30

    val currentHour = 23 // Simuliere eine Zeit innerhalb des konfigurierten Zeitraums
    mockkStatic(LocalTime::class)
    every { LocalTime.now().hour } returns currentHour

    viewModel.onPlaybackStarted()

    sleepTimer.sleepTimerActive() shouldBe true
    sleepTimer.leftSleepTimeFlow.first() shouldBe 30.minutes
  }
}

private fun book(
  name: String = "TestBook",
  lastPlayedAtMillis: Long = 0L,
  addedAtMillis: Long = 0L,
): Book {
  val chapters = listOf(
    chapter(),
    chapter(),
  )
  return Book(
    content = BookContent(
      author = UUID.randomUUID().toString(),
      name = name,
      positionInChapter = 42,
      playbackSpeed = 1F,
      addedAt = Instant.ofEpochMilli(addedAtMillis),
      chapters = chapters.map { it.id },
      cover = null,
      currentChapter = chapters.first().id,
      isActive = true,
      lastPlayedAt = Instant.ofEpochMilli(lastPlayedAtMillis),
      skipSilence = false,
      id = BookId(UUID.randomUUID().toString()),
      gain = 0F,
    ),
    chapters = chapters,
  )
}

private fun chapter(): Chapter {
  return Chapter(
    id = ChapterId("http://${UUID.randomUUID()}"),
    duration = 5.minutes.inWholeMilliseconds,
    fileLastModified = Instant.EPOCH,
    markData = emptyList(),
    name = "name",
  )
}
