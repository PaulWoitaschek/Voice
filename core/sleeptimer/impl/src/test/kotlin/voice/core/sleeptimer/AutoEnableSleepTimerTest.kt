package voice.core.sleeptimer

import androidx.datastore.core.DataStore
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import voice.core.common.DispatcherProvider
import voice.core.data.Book
import voice.core.data.BookId
import voice.core.data.repo.BookRepository
import voice.core.data.repo.BookmarkRepo
import voice.core.data.sleeptimer.SleepTimerPreference
import voice.core.playback.playstate.PlayStateManager
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

@ExperimentalCoroutinesApi
class AutoEnableSleepTimerTest {

  private lateinit var sleepTimerPreferenceStore: DataStore<SleepTimerPreference>
  private lateinit var playStateManager: PlayStateManager
  private lateinit var sleepTimer: SleepTimer
  private lateinit var bookmarkRepo: BookmarkRepo
  private lateinit var bookRepository: BookRepository
  private lateinit var currentBookStore: DataStore<BookId?>
  private val testDispatcher = StandardTestDispatcher()
  private val testScope = TestScope(testDispatcher)
  private val clock = ManualClock(Instant.parse("2023-10-26T23:00:00Z"))

  private lateinit var autoEnableSleepTimer: AutoEnableSleepTimer

  private val mockBookId = BookId("testBook")
  private val mockBook = mockk<Book>()

  @Before
  fun setUp() {
    sleepTimerPreferenceStore = mockk<DataStore<SleepTimerPreference>>(relaxUnitFun = true)
    playStateManager = mockk(relaxUnitFun = true)
    sleepTimer = mockk(relaxUnitFun = true)
    bookmarkRepo = mockk(relaxUnitFun = true) {
      coEvery { addBookmarkAtBookPosition(any(), any(), any()) } returns mockk()
    }
    bookRepository = mockk(relaxUnitFun = true)
    currentBookStore = mockk<DataStore<BookId?>>(relaxUnitFun = true)

    val dispatcherProvider = DispatcherProvider(testDispatcher, testDispatcher, testDispatcher)
    autoEnableSleepTimer = AutoEnableSleepTimer(
      sleepTimerPreferenceStore = sleepTimerPreferenceStore,
      dispatcherProvider = dispatcherProvider,
      playStateManager = playStateManager,
      sleepTimer = sleepTimer,
      bookmarkRepo = bookmarkRepo,
      bookRepository = bookRepository,
      currentBookStore = currentBookStore,
      clock = clock,
    )

    coEvery { currentBookStore.data } returns flowOf(mockBookId)
    coEvery { bookRepository.get(mockBookId) } returns mockBook
  }

  private fun mockPreferences(
    enabled: Boolean,
    startTime: LocalTime = LocalTime.of(22, 0),
    endTime: LocalTime = LocalTime.of(6, 0),
    duration: Duration = 30.minutes,
  ) {
    val preference = SleepTimerPreference(
      autoSleepTimerEnabled = enabled,
      autoSleepStartTime = startTime,
      autoSleepEndTime = endTime,
      duration = duration,
    )
    coEvery { sleepTimerPreferenceStore.data } returns flowOf(preference)
  }

  @Test
  fun `startMonitoring - when playing and should enable timer - activates timer and creates bookmark`() = testScope.runTest {
    val playStateFlow = MutableStateFlow(PlayStateManager.PlayState.Paused)
    every { playStateManager.flow } returns playStateFlow

    mockPreferences(enabled = true, startTime = LocalTime.of(22, 0), endTime = LocalTime.of(6, 0))
    coEvery { sleepTimer.sleepTimerActive() } returns false

    autoEnableSleepTimer.startMonitoring()
    playStateFlow.value = PlayStateManager.PlayState.Playing
    advanceUntilIdle()

    coVerify { sleepTimer.setActive(true) }
    coVerify { bookmarkRepo.addBookmarkAtBookPosition(book = mockBook, title = null, setBySleepTimer = true) }
  }

  @Test
  fun `startMonitoring - when playing but timer already active - does not activate timer or create bookmark`() = testScope.runTest {
    val playStateFlow = MutableStateFlow(PlayStateManager.PlayState.Paused)
    every { playStateManager.flow } returns playStateFlow

    mockPreferences(enabled = true, startTime = LocalTime.of(22, 0), endTime = LocalTime.of(6, 0))
    coEvery { sleepTimer.sleepTimerActive() } returns true

    autoEnableSleepTimer.startMonitoring()
    playStateFlow.value = PlayStateManager.PlayState.Playing
    advanceUntilIdle()

    coVerify(exactly = 0) { sleepTimer.setActive(any<Boolean>()) }
    coVerify(exactly = 0) { bookmarkRepo.addBookmarkAtBookPosition(any(), any(), any()) }
  }

  @Test
  fun `startMonitoring - when playing but auto timer disabled - does not activate timer or create bookmark`() = testScope.runTest {
    val playStateFlow = MutableStateFlow(PlayStateManager.PlayState.Paused)
    every { playStateManager.flow } returns playStateFlow

    mockPreferences(enabled = false)
    coEvery { sleepTimer.sleepTimerActive() } returns false

    autoEnableSleepTimer.startMonitoring()
    playStateFlow.value = PlayStateManager.PlayState.Playing
    advanceUntilIdle()

    coVerify(exactly = 0) { sleepTimer.setActive(any<Boolean>()) }
    coVerify(exactly = 0) { bookmarkRepo.addBookmarkAtBookPosition(any(), any(), any()) }
  }

  @Test
  fun `startMonitoring - when playing but time is out of range - does not activate timer or create bookmark`() = testScope.runTest {
    val playStateFlow = MutableStateFlow(PlayStateManager.PlayState.Paused)
    every { playStateManager.flow } returns playStateFlow

    clock.instant = LocalDateTime.now(clock).withHour(12).toInstant(ZoneOffset.UTC)

    mockPreferences(enabled = true, startTime = LocalTime.of(22, 0), endTime = LocalTime.of(6, 0))
    coEvery { sleepTimer.sleepTimerActive() } returns false

    autoEnableSleepTimer.startMonitoring()
    playStateFlow.value = PlayStateManager.PlayState.Playing
    advanceUntilIdle()

    coVerify(exactly = 0) { sleepTimer.setActive(any<Boolean>()) }
    coVerify(exactly = 0) { bookmarkRepo.addBookmarkAtBookPosition(any(), any(), any()) }
  }

  @Test
  fun `startMonitoring - when not playing - does not try to read preferences or activate timer`() = testScope.runTest {
    val playStateFlow = MutableStateFlow(PlayStateManager.PlayState.Paused)
    every { playStateManager.flow } returns playStateFlow

    coEvery { sleepTimer.sleepTimerActive() } returns false

    autoEnableSleepTimer.startMonitoring()

    playStateFlow.value = PlayStateManager.PlayState.Paused
    advanceUntilIdle()

    coVerify(exactly = 0) { sleepTimerPreferenceStore.data }
    coVerify(exactly = 0) { sleepTimer.setActive(any<Boolean>()) }
    coVerify(exactly = 0) { bookmarkRepo.addBookmarkAtBookPosition(any(), any(), any()) }
  }

  @Test
  fun `startMonitoring - when playing and createBookmark fails due to no currentBookId - timer activates, no bookmark`() =
    testScope.runTest {
      val playStateFlow = MutableStateFlow(PlayStateManager.PlayState.Paused)
      every { playStateManager.flow } returns playStateFlow

      mockPreferences(enabled = true, startTime = LocalTime.of(22, 0), endTime = LocalTime.of(6, 0))
      coEvery { sleepTimer.sleepTimerActive() } returns false
      coEvery { currentBookStore.data } returns flowOf(null)

      autoEnableSleepTimer.startMonitoring()
      playStateFlow.value = PlayStateManager.PlayState.Playing
      advanceUntilIdle()

      coVerify { sleepTimer.setActive(true) }
      coVerify(exactly = 0) { bookRepository.get(any()) }
      coVerify(exactly = 0) { bookmarkRepo.addBookmarkAtBookPosition(any(), any(), any()) }
    }

  @Test
  fun `startMonitoring - when playing and createBookmark fails due to book not found - timer activates, no bookmark`() = testScope.runTest {
    val playStateFlow = MutableStateFlow(PlayStateManager.PlayState.Paused)
    every { playStateManager.flow } returns playStateFlow

    mockPreferences(enabled = true, startTime = LocalTime.of(22, 0), endTime = LocalTime.of(6, 0))
    coEvery { sleepTimer.sleepTimerActive() } returns false
    coEvery { currentBookStore.data } returns flowOf(mockBookId)
    coEvery { bookRepository.get(mockBookId) } returns null

    autoEnableSleepTimer.startMonitoring()
    playStateFlow.value = PlayStateManager.PlayState.Playing
    advanceUntilIdle()

    coVerify { sleepTimer.setActive(true) }
    coVerify { bookRepository.get(mockBookId) }
    coVerify(exactly = 0) { bookmarkRepo.addBookmarkAtBookPosition(any(), any(), any()) }
  }

  @Test
  fun `startMonitoring - preference change while not playing - does not trigger timer`() = testScope.runTest {
    val playStateFlow = MutableStateFlow(PlayStateManager.PlayState.Paused)
    every { playStateManager.flow } returns playStateFlow

    val initialPrefs = SleepTimerPreference.Default
    val preferenceFlow = MutableStateFlow(initialPrefs)
    coEvery { sleepTimerPreferenceStore.data } returns preferenceFlow
    coEvery { sleepTimer.sleepTimerActive() } returns false

    autoEnableSleepTimer.startMonitoring()
    advanceUntilIdle()

    preferenceFlow.update {
      it.copy(autoSleepTimerEnabled = true)
    }
    advanceUntilIdle()

    coVerify(exactly = 0) { sleepTimer.setActive(any<Boolean>()) }
    coVerify(exactly = 0) { bookmarkRepo.addBookmarkAtBookPosition(any(), any(), any()) }
  }

  @Test
  fun `startMonitoring - preference change while playing but timer already active - does not re-trigger timer`() = testScope.runTest {
    val playStateFlow = MutableStateFlow(PlayStateManager.PlayState.Paused)
    every { playStateManager.flow } returns playStateFlow

    val initialPrefs = SleepTimerPreference.Default.copy(autoSleepTimerEnabled = true)
    val preferenceFlow = MutableStateFlow(initialPrefs)
    coEvery { sleepTimerPreferenceStore.data } returns preferenceFlow

    val slotIsActive = slot<Boolean>()
    coEvery { sleepTimer.sleepTimerActive() } returns false andThen true
    every { sleepTimer.setActive(capture(slotIsActive)) } just runs

    autoEnableSleepTimer.startMonitoring()

    playStateFlow.value = PlayStateManager.PlayState.Playing
    advanceUntilIdle()

    coVerify(exactly = 1) { sleepTimer.setActive(true) }
    coVerify(exactly = 1) { bookmarkRepo.addBookmarkAtBookPosition(book = mockBook, title = null, setBySleepTimer = true) }
    assertTrue(slotIsActive.captured)

    val newPrefs = SleepTimerPreference(
      autoSleepTimerEnabled = true,
      autoSleepStartTime = LocalTime.of(21, 0),
      autoSleepEndTime = LocalTime.of(5, 0),
      duration = 45.minutes,
    )
    preferenceFlow.value = newPrefs
    advanceUntilIdle()

    coVerify(exactly = 1) { sleepTimer.setActive(true) }
    coVerify(exactly = 1) { bookmarkRepo.addBookmarkAtBookPosition(any(), any(), any()) }
  }

  @Test
  fun `startMonitoring - multiple play state changes - only triggers on first play`() = testScope.runTest {
    val playStateFlow = MutableStateFlow(PlayStateManager.PlayState.Paused)
    every { playStateManager.flow } returns playStateFlow

    mockPreferences(enabled = true)
    coEvery { sleepTimer.sleepTimerActive() } returns false andThen true

    autoEnableSleepTimer.startMonitoring()

    // First play - should trigger
    playStateFlow.value = PlayStateManager.PlayState.Playing
    advanceUntilIdle()

    // Pause and play again - shouldn't trigger since timer is now active
    playStateFlow.value = PlayStateManager.PlayState.Paused
    playStateFlow.value = PlayStateManager.PlayState.Playing
    advanceUntilIdle()

    coVerify(exactly = 1) { sleepTimer.setActive(true) }
  }
}
