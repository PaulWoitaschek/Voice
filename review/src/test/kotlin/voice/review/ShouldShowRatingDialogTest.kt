package voice.review

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Test
import voice.playback.playstate.PlayStateManager
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

class ShouldShowRatingDialogTest {

  @Test
  fun `shouldShow returns false when playstate is playing`() {
    test(
      timeElapsedSinceInstallation = 100.days,
      playState = PlayStateManager.PlayState.Playing,
      ratingDialogShown = false,
      playedBookTime = 100.days,
      expected = false,
    )
  }

  @Test
  fun `shouldShow returns false when time since installation is less than 2 days`() {
    test(
      timeElapsedSinceInstallation = 1.days,
      playState = PlayStateManager.PlayState.Paused,
      ratingDialogShown = false,
      playedBookTime = 1.days,
      expected = false,
    )
  }

  @Test
  fun `shouldShow returns false when rating dialog was shown before`() {
    test(
      timeElapsedSinceInstallation = 3.days,
      playState = PlayStateManager.PlayState.Paused,
      ratingDialogShown = true,
      playedBookTime = 1.days,
      expected = false,
    )
  }

  @Test
  fun `shouldShow returns false when listened time is less than 1 day`() {
    test(
      timeElapsedSinceInstallation = 3.days,
      playState = PlayStateManager.PlayState.Paused,
      ratingDialogShown = false,
      playedBookTime = 20.hours,
      expected = false,
    )
  }

  @Test
  fun `shouldShow returns true when all conditions are satisfied`() {
    test(
      timeElapsedSinceInstallation = 3.days,
      playState = PlayStateManager.PlayState.Paused,
      ratingDialogShown = false,
      playedBookTime = 2.days,
      expected = true,
    )
  }

  private fun test(
    timeElapsedSinceInstallation: Duration,
    playState: PlayStateManager.PlayState,
    ratingDialogShown: Boolean,
    playedBookTime: Duration,
    expected: Boolean,
  ) {
    val now = Instant.ofEpochMilli(1687034006)
    val shouldShowRatingDialog = ShouldShowRatingDialog(
      installationTimeProvider = mockk {
        every { installationTime() } returns now
          .minusMillis(timeElapsedSinceInstallation.inWholeMilliseconds)
      },
      bookRepository = mockk {
        coEvery { all() } returns listOf(
          mockk {
            every { position } returns playedBookTime.inWholeMilliseconds
          },
        )
      },
      clock = Clock.fixed(now, ZoneOffset.UTC),
      playStateManager = mockk {
        every { this@mockk.playState } returns playState
      },
      ratingDialogShown = mockk {
        every { data } returns flowOf(ratingDialogShown)
      },
    )
    val showsRatingDialog = runBlocking {
      shouldShowRatingDialog.shouldShow()
    }
    showsRatingDialog shouldBe expected
  }
}
