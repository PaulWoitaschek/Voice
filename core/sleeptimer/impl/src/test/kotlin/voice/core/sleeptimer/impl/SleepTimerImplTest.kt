package voice.core.sleeptimer.impl

import io.kotest.matchers.collections.shouldBeStrictlyDecreasing
import io.kotest.matchers.collections.shouldContainOnly
import io.kotest.matchers.collections.shouldEndWith
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
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
import kotlin.time.Duration.Companion.seconds

class SleepTimerImplTest {

  private val playStateManager = PlayStateManager().apply {
    playState = PlayStateManager.PlayState.Playing
  }
  private val shakeDetector = mockk<ShakeDetector> {
    coEvery { detect() } coAnswers {
      delay(30.seconds)
    }
  }

  private val sleepTimerPreferenceStore = MemoryDataStore(SleepTimerPreference.Default)
  private val setVolumeSlots = mutableListOf<Float>()
  private val playerController = mockk<PlayerController> {
    every { setVolume(capture(setVolumeSlots)) } just Runs
    every { pauseWithRewind(any()) } answers {
      playStateManager.playState = PlayStateManager.PlayState.Paused
    }
  }

  private val fadeOutStore = MemoryDataStore(2.seconds)
  private val testDispatcher = StandardTestDispatcher()
  private val testScope = TestScope(testDispatcher)

  private val sleepTimer: SleepTimer

  init {
    val dispatcherProvider = DispatcherProvider(testDispatcher, testDispatcher, testDispatcher)
    sleepTimer = SleepTimerImpl(
      playStateManager,
      shakeDetector,
      sleepTimerPreferenceStore,
      playerController,
      fadeOutStore,
      dispatcherProvider,
    )
  }

  @Test
  fun `initial state is disabled`() {
    sleepTimer.state.value shouldBe SleepTimerState.Disabled
  }

  @Test
  fun `enable with fixed duration eventually disables and pauses playback`() = testScope.runTest {
    coEvery { shakeDetector.detect() } coAnswers {
      suspendCancellableCoroutine { } // never completes
    }

    sleepTimer.enable(SleepTimerMode.TimedWithDuration(1.seconds))

    advanceTimeBy(2.seconds)
    sleepTimer.state.value shouldBe SleepTimerState.Disabled
    coVerify(exactly = 1) { playerController.pauseWithRewind(any()) }
  }

  @Test
  fun `enable with EndOfChapter sets state`() = testScope.runTest {
    sleepTimer.enable(SleepTimerMode.EndOfChapter)

    advanceTimeBy(1)
    sleepTimer.state.value shouldBe SleepTimerState.Enabled.WithEndOfChapter
  }

  @Test
  fun `disable cancels timer and resets state`() = testScope.runTest {
    sleepTimer.enable(SleepTimerMode.TimedWithDuration(5.seconds))
    advanceTimeBy(1.seconds)

    sleepTimer.disable()

    sleepTimer.state.value shouldBe SleepTimerState.Disabled
  }

  @Test
  fun withDurationResetsVolume() = testScope.runTest {
    sleepTimer.enable(SleepTimerMode.TimedWithDuration(5.seconds))
    advanceTimeBy(3.seconds)
    yield()

    // after the first 3 seconds, the volume should not have been decreased
    setVolumeSlots.shouldContainOnly(1F)

    setVolumeSlots.clear()
    advanceTimeBy(1.seconds)
    yield()
    // now we're in fade-out phase, volume should decrease
    setVolumeSlots.shouldNotBeEmpty()
      .shouldBeStrictlyDecreasing()

    // after the timer finished, volume should be reset
    setVolumeSlots.clear()
    advanceTimeBy(2.seconds)
    yield()
    setVolumeSlots.shouldEndWith(1f)
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
