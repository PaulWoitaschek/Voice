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

class ShouldShowReviewDialogTest {

  @Test
  fun `shouldShow returns false when playstate is playing`() {
    test(
      timeElapsedSinceInstallation = 100.days,
      playState = PlayStateManager.PlayState.Playing,
      reviewDialogShown = false,
      playedBookTime = 100.days,
      expected = false,
    )
  }

  @Test
  fun `shouldShow returns false when time since installation is less than 2 days`() {
    test(
      timeElapsedSinceInstallation = 1.days,
      playState = PlayStateManager.PlayState.Paused,
      reviewDialogShown = false,
      playedBookTime = 1.days,
      expected = false,
    )
  }

  @Test
  fun `shouldShow returns false when review dialog was shown before`() {
    test(
      timeElapsedSinceInstallation = 3.days,
      playState = PlayStateManager.PlayState.Paused,
      reviewDialogShown = true,
      playedBookTime = 1.days,
      expected = false,
    )
  }

  @Test
  fun `shouldShow returns false when listened time is less than 5 hours`() {
    test(
      timeElapsedSinceInstallation = 3.days,
      playState = PlayStateManager.PlayState.Paused,
      reviewDialogShown = false,
      playedBookTime = 2.hours,
      expected = false,
    )
  }

  @Test
  fun `shouldShow returns true when all conditions are satisfied`() {
    test(
      timeElapsedSinceInstallation = 3.days,
      playState = PlayStateManager.PlayState.Paused,
      reviewDialogShown = false,
      playedBookTime = 2.days,
      expected = true,
    )
  }

  private fun test(
    timeElapsedSinceInstallation: Duration,
    playState: PlayStateManager.PlayState,
    reviewDialogShown: Boolean,
    playedBookTime: Duration,
    expected: Boolean,
  ) {
    val now = Instant.ofEpochMilli(1687034006)
    val shouldShowReviewDialog = ShouldShowReviewDialog(
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
      reviewDialogShown = mockk {
        every { data } returns flowOf(reviewDialogShown)
      },
      reviewTranslated = mockk {
        every { translated() } returns true
      },
    )
    val showsReviewDialog = runBlocking {
      shouldShowReviewDialog.shouldShow()
    }
    showsReviewDialog shouldBe expected
  }
}
