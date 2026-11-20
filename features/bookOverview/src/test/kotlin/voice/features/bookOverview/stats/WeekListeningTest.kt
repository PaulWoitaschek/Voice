package voice.features.bookOverview.stats

import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import voice.core.data.repo.ListeningStatsRepo

class WeekListeningTest {

  private val monday = LocalDate.of(2026, 11, 16)
  private var now = Instant.parse("2026-11-22T23:59:30Z")
  private val thisWeek = MutableStateFlow<Duration?>(2.hours)
  private val repo =
    mockk<ListeningStatsRepo> {
      every { listened(monday, monday.plusWeeks(1)) } returns thisWeek
      every { listened(monday.plusWeeks(1), monday.plusWeeks(2)) } returns flowOf(Duration.ZERO)
    }
  private val weekListening =
    WeekListening(
      repo = repo,
      clock =
        object : Clock {
          override fun now() = now
        },
      zone = { ZoneOffset.UTC },
      firstDayOfWeek = { DayOfWeek.MONDAY },
    )

  @Test
  fun `2 h listened by Sunday night, midnight passes, 0 min this week`() = runTest {
    weekListening.listened().test {
      assertEquals(expected = 2.hours, actual = awaitItem())
      now = Instant.parse("2026-11-23T00:00:30Z")

      assertEquals(expected = Duration.ZERO, actual = awaitItem())
    }
  }

  @Test
  fun `playback saves every few seconds, the time only changes with each whole minute`() = runTest {
    weekListening.listened().test {
      assertEquals(expected = 2.hours, actual = awaitItem())
      thisWeek.value = 2.hours + 30.seconds
      thisWeek.value = 2.hours + 59.seconds
      thisWeek.value = 2.hours + 1.minutes + 10.seconds

      assertEquals(expected = 2.hours + 1.minutes, actual = awaitItem())
    }
  }
}
