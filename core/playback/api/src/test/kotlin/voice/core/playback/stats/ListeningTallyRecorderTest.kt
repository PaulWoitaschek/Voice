package voice.core.playback.stats

import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.just
import io.mockk.mockk
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import voice.core.data.BookId
import voice.core.data.ListeningTally
import voice.core.data.repo.ListeningStatsRepo

class ListeningTallyRecorderTest {

  private val scope = TestScope()
  private val tallies = mutableListOf<ListeningTally>()
  private val repo =
    mockk<ListeningStatsRepo> {
      coEvery { add(capture(tallies)) } just Runs
    }
  private var now = Instant.parse("2026-10-07T21:40:00Z")
  private val recorder =
    ListeningTallyRecorder(
      repo = repo,
      clock =
        object : Clock {
          override fun now(): Instant = now
        },
      zone = { ZoneId.of("Europe/Berlin") },
      scope = scope.backgroundScope,
    )
  private val dune = BookId("dune")

  @Test
  fun `a number counts on the local day of the listener`() = scope.runTest {
    recorder.add(dune, "sleep_timer_night")
    runCurrent()

    assertEquals(
      expected =
        listOf(ListeningTally(LocalDate.parse("2026-10-07"), dune, "sleep_timer_night", 1)),
      actual = tallies,
    )
  }

  @Test
  fun `a stop after midnight counts on the new day`() = scope.runTest {
    // 00:10 in Berlin
    now = Instant.parse("2026-10-07T22:10:00Z")

    recorder.add(dune, "sleep_timer_night")
    runCurrent()

    assertEquals(expected = LocalDate.parse("2026-10-08"), actual = tallies.single().day)
  }
}
