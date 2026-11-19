package voice.core.playback.stats

import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import voice.core.analytics.api.Analytics
import voice.core.data.repo.ListeningStatsRepo
import voice.core.data.repo.ListeningSummary

class ListeningUserPropertiesTest {

  private val scope = TestScope()
  private val analytics = mockk<Analytics>(relaxUnitFun = true)
  private val repo = mockk<ListeningStatsRepo>()
  private val now = Instant.parse("2026-10-07T10:00:00Z")

  private fun TestScope.startTheApp(
    listened: Duration,
    audio: Duration,
  ) {
    coEvery { repo.summary(from = now - 30.days) } returns
      ListeningSummary(
        listened = listened,
        audio = audio,
        listeningDays = 0,
        finishedBooks = emptyList(),
      )
    ListeningUserProperties(
        analytics = analytics,
        repo = repo,
        clock =
          object : Clock {
            override fun now(): Instant = now
          },
        scope = backgroundScope,
      )
      .onAppStart(mockk())
    runCurrent()
  }

  @Test
  fun `analytics on, in the last 30 days 8 h at 1x and 4 h at 2x, start the app, the time 10 to 20 h, the speed up to 1_5x`() =
    scope.runTest {
      startTheApp(listened = 8.hours + 4.hours, audio = 8.hours + 4.hours * 2)

      verify { analytics.setUserProperty("listening_time", "10_to_20_h") }
      verify { analytics.setUserProperty("listening_speed", "up_to_1_5x") }
    }

  @Test
  fun `analytics on, in the last 30 days 30 minutes at 3_5x, start the app, the time under 1 h, the speed faster than 3x`() =
    scope.runTest {
      startTheApp(listened = 30.minutes, audio = 30.minutes * 3.5)

      verify { analytics.setUserProperty("listening_time", "under_1_h") }
      verify { analytics.setUserProperty("listening_speed", "faster_than_3x") }
    }

  @Test
  fun `analytics on, in the last 30 days 3 h at 1x, start the app, the time 2 to 5 h, the speed 1x`() =
    scope.runTest {
      startTheApp(listened = 3.hours, audio = 3.hours)

      verify { analytics.setUserProperty("listening_time", "2_to_5_h") }
      verify { analytics.setUserProperty("listening_speed", "1x") }
    }

  @Test
  fun `analytics on, the last listening was 2 months ago, start the app, the time none, no speed`() =
    scope.runTest {
      startTheApp(listened = Duration.ZERO, audio = Duration.ZERO)

      verify { analytics.setUserProperty("listening_time", "none") }
      verify { analytics.setUserProperty("listening_speed", "none") }
    }

  @Test
  fun `only ranges are reported, and each of them only once`() = scope.runTest {
    startTheApp(listened = 3.hours, audio = 3.hours)

    verify(exactly = 2) { analytics.setUserProperty(any(), any()) }
    verify(exactly = 0) { analytics.event(any(), any()) }
  }

  @Test
  fun `each time range starts at its lower limit`() {
    listOf(
        Duration.ZERO to "none",
        1.milliseconds to "under_1_h",
        1.hours - 1.milliseconds to "under_1_h",
        1.hours to "1_to_2_h",
        2.hours - 1.milliseconds to "1_to_2_h",
        2.hours to "2_to_5_h",
        5.hours - 1.milliseconds to "2_to_5_h",
        5.hours to "5_to_10_h",
        10.hours - 1.milliseconds to "5_to_10_h",
        10.hours to "10_to_20_h",
        20.hours - 1.milliseconds to "10_to_20_h",
        20.hours to "20_to_40_h",
        40.hours - 1.milliseconds to "20_to_40_h",
        40.hours to "40_to_70_h",
        70.hours - 1.milliseconds to "40_to_70_h",
        70.hours to "70_to_100_h",
        100.hours - 1.milliseconds to "70_to_100_h",
        100.hours to "100_to_150_h",
        150.hours - 1.milliseconds to "100_to_150_h",
        150.hours to "above_150_h",
      )
      .forEach { (listened, range) ->
        assertEquals(expected = range, actual = timeRange(listened), message = "$listened")
      }
  }

  @Test
  fun `each speed range ends at its upper limit`() {
    listOf(
        0.94 to "slower_than_1x",
        0.95 to "1x",
        1.05 to "1x",
        1.06 to "up_to_1_25x",
        1.25 to "up_to_1_25x",
        1.254 to "up_to_1_25x",
        1.26 to "up_to_1_5x",
        1.5 to "up_to_1_5x",
        1.51 to "up_to_1_75x",
        1.75 to "up_to_1_75x",
        1.76 to "up_to_2x",
        2.0 to "up_to_2x",
        2.01 to "up_to_2_5x",
        2.5 to "up_to_2_5x",
        2.51 to "up_to_3x",
        3.0 to "up_to_3x",
        3.01 to "faster_than_3x",
      )
      .forEach { (speed, range) ->
        assertEquals(expected = range, actual = speedRange(speed), message = "$speed")
      }
  }
}
