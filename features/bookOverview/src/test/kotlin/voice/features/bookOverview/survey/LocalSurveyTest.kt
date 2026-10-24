package voice.features.bookOverview.survey

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import voice.core.common.AppInfoProvider
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LocalSurveyTest {

  @Test
  fun `the free build gets the survey on its last day`() = runTest {
    assertEquals("https://tally.so/r/5XBPZN", localSurvey(flavor = "free", now = "2026-11-13T23:00:00Z").url())
  }

  @Test
  fun `the free build no longer gets the survey once it ended`() = runTest {
    assertNull(localSurvey(flavor = "free", now = "2026-11-14T00:00:00Z").url())
  }

  @Test
  fun `the play build never gets the survey`() = runTest {
    assertNull(localSurvey(flavor = "play", now = "2026-10-24T10:00:00Z").url())
  }

  private fun localSurvey(
    flavor: String,
    now: String,
  ): LocalSurvey {
    return LocalSurvey(
      appInfoProvider = mockk<AppInfoProvider> {
        every { this@mockk.flavor } returns flavor
      },
      clock = Clock.fixed(Instant.parse(now), ZoneOffset.UTC),
    )
  }
}
