package voice.core.sleeptimer.impl

import io.mockk.Runs
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import voice.core.common.DispatcherProvider
import voice.core.data.ListeningEvent
import voice.core.playback.PlayerController
import voice.core.playback.playstate.PlayStateManager
import voice.core.sleeptimer.FadeOutStore
import voice.core.sleeptimer.RestartTrigger
import voice.core.sleeptimer.ShakeDetector
import voice.core.sleeptimer.SleepTimer
import voice.core.sleeptimer.SleepTimerImpl
import voice.core.sleeptimer.SleepTimerMode
import voice.core.sleeptimer.SleepTimerPreference
import voice.core.sleeptimer.SleepTimerPreferenceStore
import voice.core.sleeptimer.SleepTimerState
import voice.core.sleeptimer.SleepTimerTracker

private class TestShakeDetector : ShakeDetector {
  private val shakes = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
  var detecting = false
    private set

  override suspend fun detect() {
    detecting = true
    try {
      shakes.first()
    } finally {
      detecting = false
    }
  }

  fun shake() {
    shakes.tryEmit(Unit)
  }
}

class SleepTimerImplTest {

  private val playStateManager =
    PlayStateManager().apply {
      playState = PlayStateManager.PlayState.Playing
    }
  private val shakeDetector = TestShakeDetector()
  private val sleepTimerPreferenceStore = MemoryDataStore(SleepTimerPreference.Default)
  private val setVolumeSlots = mutableListOf<Float>()
  private val playerController =
    mockk<PlayerController> {
      every { setVolume(capture(setVolumeSlots)) } just Runs
      every { pauseWithRewind(any()) } answers
        {
          playStateManager.playState = PlayStateManager.PlayState.Paused
        }
      every { record(any(), any()) } just Runs
      every {
        play(any())
      } answers
        {
          playStateManager.playState = PlayStateManager.PlayState.Playing
        }
    }

  private val fadeOutStore = MemoryDataStore(2.seconds)
  private val tracker = mockk<SleepTimerTracker>(relaxed = true)
  private val testDispatcher = StandardTestDispatcher()
  private val testScope = TestScope(testDispatcher)

  private val sleepTimer: SleepTimer

  init {
    val dispatcherProvider = DispatcherProvider(testDispatcher, testDispatcher, testDispatcher)
    sleepTimer =
      SleepTimerImpl(
        playStateManager,
        shakeDetector,
        SleepTimerPreferenceStore(sleepTimerPreferenceStore),
        playerController,
        FadeOutStore(fadeOutStore),
        dispatcherProvider,
        tracker,
      )
  }

  @Test
  fun `initial state is disabled`() {
    assertEquals(expected = SleepTimerState.Disabled, actual = sleepTimer.state.value)
  }

  @Test
  fun `enable with fixed duration eventually disables and pauses playback`() = testScope.runTest {
    sleepTimer.enable(SleepTimerMode.TimedWithDuration(1.seconds))

    advanceTimeBy(2.seconds)
    assertEquals(expected = SleepTimerState.Disabled, actual = sleepTimer.state.value)
    coVerify(exactly = 1) { playerController.pauseWithRewind(any()) }
  }

  @Test
  fun `enable with EndOfChapter sets state`() = testScope.runTest {
    sleepTimer.enable(SleepTimerMode.EndOfChapter)

    advanceTimeBy(1)
    assertEquals(expected = SleepTimerState.Enabled.WithEndOfChapter, actual = sleepTimer.state.value)
  }

  @Test
  fun `turning the timer on goes into the history`() = testScope.runTest {
    sleepTimer.enable(SleepTimerMode.EndOfChapter)
    sleepTimer.enable(SleepTimerMode.TimedWithDefault)

    verify {
      playerController.record(ListeningEvent.Type.SleepTimerStarted, ListeningEvent.Source.App)
      playerController.record(ListeningEvent.Type.SleepTimerStarted, ListeningEvent.Source.SleepTimer)
    }
  }

  @Test
  fun `disable cancels timer and resets state`() = testScope.runTest {
    sleepTimer.enable(SleepTimerMode.TimedWithDuration(5.seconds))
    advanceTimeBy(1.seconds)

    sleepTimer.disable()

    assertEquals(expected = SleepTimerState.Disabled, actual = sleepTimer.state.value)
  }

  @Test
  fun withDurationResetsVolume() = testScope.runTest {
    sleepTimer.enable(SleepTimerMode.TimedWithDuration(5.seconds))
    advanceTimeBy(3.seconds)
    yield()

    assertEquals(expected = setOf(1F), actual = setVolumeSlots.toSet())

    setVolumeSlots.clear()
    advanceTimeBy(1.seconds)
    yield()
    assertTrue(setVolumeSlots.isNotEmpty())
    assertTrue(setVolumeSlots.zipWithNext().all { (previous, next) -> previous > next })

    setVolumeSlots.clear()
    advanceTimeBy(2.seconds)
    yield()
    assertEquals(expected = 1f, actual = setVolumeSlots.last())
  }

  @Test
  fun shake_does_not_cancel_second_countdown_after_window() = testScope.runTest {
    // Use a LONG duration so we can observe behavior across the 30s window
    val longDuration = SleepTimerImpl.SHAKE_TO_RESET_TIME * 2

    sleepTimer.enable(SleepTimerMode.TimedWithDuration(longDuration))

    // 1) Let the first countdown finish and enter the shake window
    advanceTimeBy(longDuration + 1.seconds)
    runCurrent()
    coVerify(exactly = 1) { playerController.pauseWithRewind(any()) }
    assertEquals(expected = SleepTimerState.Disabled, actual = sleepTimer.state.value)

    // 2) Trigger the shake → a new countdown should start independently of the old timeout
    shakeDetector.shake()
    runCurrent()
    verify(exactly = 1) { playerController.play(source = null) }
    assertEquals(expected = SleepTimerState.Enabled.WithDuration(longDuration), actual = sleepTimer.state.value)

    // 3) Advance past the original 30s shake window and allow the second countdown to finish
    advanceTimeBy(SleepTimerImpl.SHAKE_TO_RESET_TIME + longDuration + 2.seconds)
    runCurrent()

    coVerify(exactly = 2) { playerController.pauseWithRewind(any()) }
    assertEquals(expected = SleepTimerState.Disabled, actual = sleepTimer.state.value)
    verify(exactly = 1) { tracker.restarted(RestartTrigger.ShakeAfterPause) }
  }

  @Test
  fun `shaking during the fade-out restarts the timer without pausing`() = testScope.runTest {
    sleepTimer.enable(SleepTimerMode.TimedWithDuration(10.seconds))
    advanceTimeBy(9.seconds)
    runCurrent()
    assertTrue(shakeDetector.detecting)

    shakeDetector.shake()
    advanceTimeBy(200.milliseconds)
    runCurrent()

    assertEquals(expected = SleepTimerState.Enabled.WithDuration(10.seconds), actual = sleepTimer.state.value)
    assertEquals(expected = 1F, actual = setVolumeSlots.last())
    verify(exactly = 0) { playerController.pauseWithRewind(any()) }
    verify(exactly = 0) { playerController.play(any()) }
    verify { playerController.record(ListeningEvent.Type.SleepTimerExtended, ListeningEvent.Source.SleepTimer) }
    verify { tracker.restarted(RestartTrigger.ShakeWhileFading) }
  }

  @Test
  fun `shaking before the fade-out does nothing`() = testScope.runTest {
    sleepTimer.enable(SleepTimerMode.TimedWithDuration(10.seconds))
    advanceTimeBy(5.seconds)
    runCurrent()
    assertFalse(shakeDetector.detecting)

    shakeDetector.shake()
    advanceTimeBy(5.seconds + 100.milliseconds)
    runCurrent()

    assertEquals(expected = SleepTimerState.Disabled, actual = sleepTimer.state.value)
    verify(exactly = 1) { playerController.pauseWithRewind(any()) }
    verify(exactly = 0) { tracker.restarted(any()) }
  }

  @Test
  fun `pressing play right after the timer ended restarts it`() = testScope.runTest {
    sleepTimer.enable(SleepTimerMode.TimedWithDuration(5.seconds))
    advanceTimeBy(6.seconds)
    runCurrent()
    assertEquals(expected = SleepTimerState.Disabled, actual = sleepTimer.state.value)

    playStateManager.playState = PlayStateManager.PlayState.Playing
    runCurrent()

    assertEquals(expected = SleepTimerState.Enabled.WithDuration(5.seconds), actual = sleepTimer.state.value)
    verify(exactly = 0) { playerController.play(any()) }
    verify { playerController.record(ListeningEvent.Type.SleepTimerStarted, ListeningEvent.Source.SleepTimer) }
    verify { tracker.restarted(RestartTrigger.Play) }
  }

  @Test
  fun `a pause that takes effect late is not mistaken for pressing play`() = testScope.runTest {
    every { playerController.pauseWithRewind(any()) } just Runs
    sleepTimer.enable(SleepTimerMode.TimedWithDuration(5.seconds))
    advanceTimeBy(6.seconds)
    runCurrent()
    assertEquals(expected = SleepTimerState.Disabled, actual = sleepTimer.state.value)

    playStateManager.playState = PlayStateManager.PlayState.Paused
    runCurrent()
    assertEquals(expected = SleepTimerState.Disabled, actual = sleepTimer.state.value)

    playStateManager.playState = PlayStateManager.PlayState.Playing
    runCurrent()
    assertEquals(expected = SleepTimerState.Enabled.WithDuration(5.seconds), actual = sleepTimer.state.value)
  }

  @Test
  fun `pressing play after the window keeps playing without a timer`() = testScope.runTest {
    sleepTimer.enable(SleepTimerMode.TimedWithDuration(5.seconds))
    advanceTimeBy(6.seconds + SleepTimerImpl.SHAKE_TO_RESET_TIME)
    runCurrent()

    playStateManager.playState = PlayStateManager.PlayState.Playing
    runCurrent()

    assertEquals(expected = SleepTimerState.Disabled, actual = sleepTimer.state.value)
    verify(exactly = 0) { tracker.restarted(any()) }
  }
}
