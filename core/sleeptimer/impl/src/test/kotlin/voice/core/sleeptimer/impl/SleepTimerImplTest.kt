package voice.core.sleeptimer.impl

import androidx.datastore.core.DataStore
import io.kotest.matchers.collections.shouldBeSortedDescending
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.floats.shouldBeExactly
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import voice.core.common.DispatcherProvider
import voice.core.data.sleeptimer.SleepTimerPreference
import voice.core.logging.core.LogWriter
import voice.core.logging.core.Logger
import voice.core.playback.PlayerController
import voice.core.playback.playstate.PlayStateManager
import voice.core.sleeptimer.ShakeDetector
import voice.core.sleeptimer.SleepTimer
import voice.core.sleeptimer.SleepTimerImpl
import voice.core.sleeptimer.SleepTimerMode
import voice.core.sleeptimer.SleepTimerState
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class SleepTimerImplTest {

  private lateinit var playStateManager: PlayStateManager
  private val shakeDetector: ShakeDetector = mockk()
  private lateinit var sleepTimerPreferenceStore: DataStore<SleepTimerPreference>
  private val setVolumeSlot = mutableListOf<Float>()
  private val playerController = mockk<PlayerController> {
    every { setVolume(capture(setVolumeSlot)) } just Runs
    every { pauseWithRewind(any()) } just Runs
    every { play() } just Runs
  }
  private val fadeOutStore: DataStore<Duration> = mockk {
    every { data } returns flowOf(5.seconds)
  }
  private val testDispatcher = StandardTestDispatcher()
  private val testScope = TestScope(testDispatcher)

  private lateinit var sleepTimer: SleepTimer
  private val playStateFlow = MutableStateFlow(PlayStateManager.PlayState.Playing)

  @Before
  fun setUp() {
    playStateManager = mockk(relaxed = true) {
      every { flow } returns playStateFlow
      every { playState } returns PlayStateManager.PlayState.Playing
    }
    sleepTimerPreferenceStore = mockk(relaxed = true)

    val dispatcherProvider = DispatcherProvider(testDispatcher, testDispatcher, testDispatcher)
    sleepTimer = SleepTimerImpl(
      playStateManager = playStateManager,
      shakeDetector = shakeDetector,
      sleepTimerPreferenceStore = sleepTimerPreferenceStore,
      playerController = playerController,
      fadeOutStore = fadeOutStore,
      dispatcherProvider = dispatcherProvider,
    )
  }

  @Test
  fun `initial state is disabled`() {
    assertEquals(SleepTimerState.Disabled, sleepTimer.state.value)
    assertFalse(sleepTimer.state.value.enabled)
  }

  @Test
  fun `enable with TimedWithDuration starts countdown and updates state`() = testScope.runTest {
    sleepTimer.enable(SleepTimerMode.TimedWithDuration(2.seconds))
    // immediately enabled
    yield()
    sleepTimer.state.value.shouldBeInstanceOf<SleepTimerState.Enabled.WithDuration>()

    // simulate 1 second passing
    advanceTimeBy(1000)
    val state = sleepTimer.state.value
    state.shouldBeInstanceOf<SleepTimerState.Enabled.WithDuration>()
      .leftDuration.shouldBeLessThan(2.seconds)

    setVolumeSlot.last().shouldBeLessThan(1f)
  }

  @Test
  fun `enable with TimedWithDefault uses preference duration`() = testScope.runTest {
    val prefDuration = 30.minutes
    val preference = SleepTimerPreference.Default.copy(duration = prefDuration)
    coEvery { sleepTimerPreferenceStore.data } returns flowOf(preference)

    sleepTimer.enable(SleepTimerMode.TimedWithDefault)
    advanceTimeBy(1)

    val state = sleepTimer.state.value
    assertTrue(state is SleepTimerState.Enabled.WithDuration)
    assertEquals(prefDuration, (state as SleepTimerState.Enabled.WithDuration).leftDuration)
  }

  @Test
  fun `enable with EndOfChapter sets correct state`() = testScope.runTest {
    sleepTimer.enable(SleepTimerMode.EndOfChapter)
    advanceTimeBy(1)

    assertEquals(SleepTimerState.Enabled.WithEndOfChapter, sleepTimer.state.value)
  }

  @Test
  fun `disable cancels timer and resets state`() = testScope.runTest {
    sleepTimer.enable(SleepTimerMode.TimedWithDuration(5.seconds))
    advanceTimeBy(1)
    assertTrue(sleepTimer.state.value.enabled)

    sleepTimer.disable()

    assertEquals(SleepTimerState.Disabled, sleepTimer.state.value)
    setVolumeSlot.last().shouldBeExactly(1f)
  }

  @Test
  fun `enable cancels previous job before starting new one`() = testScope.runTest {
    sleepTimer.enable(SleepTimerMode.TimedWithDuration(30.seconds))
    advanceTimeBy(1)
    sleepTimer.enable(SleepTimerMode.EndOfChapter)
    advanceTimeBy(1)

    assertEquals(SleepTimerState.Enabled.WithEndOfChapter, sleepTimer.state.value)
  }

  @Test
  fun `volume fades out as time approaches fadeOutDuration`() = testScope.runTest {
    coEvery { shakeDetector.detect() } coAnswers {
      // suspend forever → simulate no shake
      suspendCancellableCoroutine { }
    }
    val duration = 4.seconds // less than fadeOutDuration (5s)
    sleepTimer.enable(SleepTimerMode.TimedWithDuration(duration))
    advanceTimeBy(100) // initial tick

    // should be fading → verify multiple setVolume calls with values < 1F
    advanceTimeBy(5000)

    verifySetVolumeWasSetDecreasingly()

    advanceTimeBy(30.seconds)
    setVolumeSlot.last() shouldBeExactly 1f
  }

  private fun verifySetVolumeWasSetDecreasingly() {
    setVolumeSlot.shouldNotBeEmpty()
    setVolumeSlot.first().shouldBeGreaterThan(setVolumeSlot.last())
    setVolumeSlot.shouldBeSortedDescending()
  }

  @Test
  fun `timer expires and pauses playback when no shake detected`() = testScope.runTest {
    coEvery { shakeDetector.detect() } coAnswers {
      // suspend forever → simulate no shake
      suspendCancellableCoroutine { }
    }

    sleepTimer.enable(SleepTimerMode.TimedWithDuration(1.seconds))
    advanceTimeBy(2000) // beyond expiry

    // should pause and reset state
    coVerify { playerController.pauseWithRewind(5.seconds) }
    assertEquals(SleepTimerState.Disabled, sleepTimer.state.value)
  }

  @Test
  fun `shake within timeout resets timer`() = testScope.runTest {
    val duration = 1.seconds
    coEvery { shakeDetector.detect() } coAnswers {
      // simulate shake detected immediately
    }

    sleepTimer.enable(SleepTimerMode.TimedWithDuration(duration))
    advanceTimeBy(2000) // expire + shake reset

    coVerifyOrder {
      playerController.pauseWithRewind(5.seconds) // at expiry
      playerController.play() // after shake
    }
    assertTrue(sleepTimer.state.value is SleepTimerState.Enabled.WithDuration)
  }

  @Test
  fun `countdown pauses when playback is not playing`() = testScope.runTest {
    val duration = 2.seconds
    playStateFlow.value = PlayStateManager.PlayState.Paused
    every { playStateManager.playState } returns PlayStateManager.PlayState.Paused

    sleepTimer.enable(SleepTimerMode.TimedWithDuration(duration))
    advanceTimeBy(2000)

    // since paused, timer shouldn't have decremented
    val state = sleepTimer.state.value
    assertTrue(state is SleepTimerState.Enabled.WithDuration)
    assertEquals(duration, (state as SleepTimerState.Enabled.WithDuration).leftDuration)
  }

  companion object {

    @BeforeClass
    @JvmStatic
    fun setup() {
      Logger.install(
        object : LogWriter {
          override fun log(
            severity: Logger.Severity,
            message: String,
            throwable: Throwable?,
          ) {
            println(
              buildString {
                append("${severity.name}: ")
                append(message)
                if (throwable != null) {
                  append(", $throwable")
                }
              },
            )
          }
        },
      )
    }
  }
}
