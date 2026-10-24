package voice.core.sleeptimer

import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import voice.core.playback.playstate.PlayStateManager
import voice.core.sleeptimer.impl.MemoryDataStore
import java.time.Clock
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import kotlin.test.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

class AutoEnableSleepTimerMinimalTest {
  private val testDispatcher = StandardTestDispatcher()
  private val testScope = TestScope(testDispatcher)
  private val sleepTimerPreferenceStore = MemoryDataStore(SleepTimerPreference.Default)
  private val playStateManager = PlayStateManager()
  private val sleepTimer = mockk<SleepTimer>(relaxUnitFun = true) {
    every { state } returns MutableStateFlow(SleepTimerState.Disabled)
  }

  private fun prefs(
    enabled: Boolean,
    start: LocalTime = LocalTime.of(22, 0),
    end: LocalTime = LocalTime.of(6, 0),
    duration: Duration = 30.minutes,
  ) = SleepTimerPreference(
    autoSleepTimerEnabled = enabled,
    autoSleepStartTime = start,
    autoSleepEndTime = end,
    duration = duration,
  )

  private val sut = AutoEnableSleepTimer(
    sleepTimerPreferenceStore = SleepTimerPreferenceStore(sleepTimerPreferenceStore),
    playStateManager = playStateManager,
    sleepTimer = sleepTimer,
    clock = Clock.fixed(Instant.parse("2020-01-01T23:00:00Z"), ZoneId.of("UTC")),
    scope = testScope.backgroundScope,
  )

  @Test
  fun `enables when playing and in time window`() = testScope.runTest {
    sleepTimerPreferenceStore.updateData { prefs(enabled = true) }

    sut.onAppStart(mockk())
    playStateManager.playState = PlayStateManager.PlayState.Playing
    advanceUntilIdle()
    yield()

    coVerify { sleepTimer.enable(SleepTimerMode.TimedWithDefault) }
  }

  @Test
  fun `does nothing when sleeptimer is disabled`() = testScope.runTest {
    sleepTimerPreferenceStore.updateData { prefs(enabled = false) }

    sut.onAppStart(mockk())
    playStateManager.playState = PlayStateManager.PlayState.Playing
    advanceUntilIdle()
    yield()

    coVerify(exactly = 0) { sleepTimer.enable(any()) }
  }
}
