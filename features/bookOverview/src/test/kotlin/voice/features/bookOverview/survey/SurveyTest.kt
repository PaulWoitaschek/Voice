package voice.features.bookOverview.survey

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import voice.core.analytics.api.Analytics
import voice.core.common.AppInfoProvider
import voice.core.featureflag.MemoryFeatureFlag
import voice.features.bookOverview.MemoryDataStore
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.toKotlinInstant

class SurveyTest {

  private val today = LocalDate.of(2026, 10, 20).toEpochDay()
  private val yesterday = today - 1
  private val analytics = mockk<Analytics>(relaxUnitFun = true)

  @Test
  fun `the remote survey comes before the local one`() = runTest {
    val survey = survey(remoteUrl = REMOTE, localUrl = LOCAL)

    assertEquals(REMOTE, survey.url().first())
  }

  @Test
  fun `without a remote survey the local one shows`() = runTest {
    val survey = survey(remoteUrl = "", localUrl = LOCAL)

    assertEquals(LOCAL, survey.url().first())
  }

  @Test
  fun `the local survey follows a handled remote one on the next day`() = runTest {
    val survey = survey(remoteUrl = REMOTE, localUrl = LOCAL, handled = mapOf(REMOTE to yesterday))

    assertEquals(LOCAL, survey.url().first())
  }

  @Test
  fun `no survey shows on the day one was handled`() = runTest {
    val survey = survey(remoteUrl = REMOTE, localUrl = LOCAL, handled = mapOf(REMOTE to today))

    assertNull(survey.url().first())
  }

  @Test
  fun `no survey shows once all are handled`() = runTest {
    val survey = survey(remoteUrl = REMOTE, localUrl = LOCAL, handled = mapOf(REMOTE to yesterday, LOCAL to yesterday))

    assertNull(survey.url().first())
  }

  @Test
  fun `opening a survey adds the app version, installer and days installed and marks it handled today`() = runTest {
    val store = MemoryDataStore<Map<String, Long>>(emptyMap())
    val survey = survey(remoteUrl = REMOTE, store = store)

    assertEquals("$REMOTE?app_version=1.2.3&source=com.android.vending&days_installed=14", survey.open(REMOTE))
    assertEquals(mapOf(REMOTE to today), store.data.first())
    verify { analytics.event("survey_card", mapOf("action" to "open")) }
  }

  @Test
  fun `opening a survey with a query keeps it`() = runTest {
    val survey = survey(remoteUrl = "$REMOTE?lang=de")

    assertEquals("$REMOTE?lang=de&app_version=1.2.3&source=com.android.vending&days_installed=14", survey.open("$REMOTE?lang=de"))
  }

  @Test
  fun `opening a survey without a known installer says so`() = runTest {
    val survey = survey(remoteUrl = REMOTE, installer = null)

    assertEquals("$REMOTE?app_version=1.2.3&source=unknown&days_installed=14", survey.open(REMOTE))
  }

  @Test
  fun `dismissing a survey marks it handled today`() = runTest {
    val store = MemoryDataStore<Map<String, Long>>(emptyMap())
    val survey = survey(localUrl = LOCAL, store = store)

    survey.dismiss(LOCAL)

    assertEquals(mapOf(LOCAL to today), store.data.first())
    assertNull(survey.url().first())
    verify { analytics.event("survey_card", mapOf("action" to "dismiss")) }
  }

  private fun survey(
    remoteUrl: String = "",
    localUrl: String? = null,
    handled: Map<String, Long> = emptyMap(),
    store: MemoryDataStore<Map<String, Long>> = MemoryDataStore(handled),
    installer: String? = "com.android.vending",
  ): Survey {
    return Survey(
      surveyUrlFeatureFlag = SurveyUrlFeatureFlag(MemoryFeatureFlag(remoteUrl)),
      handledSurveysStore = HandledSurveysStore(store),
      localSurvey = mockk {
        coEvery { url() } returns localUrl
      },
      clock = Clock.fixed(Instant.parse("2026-10-20T10:00:00Z"), ZoneOffset.UTC),
      appInfoProvider = mockk<AppInfoProvider> {
        every { versionName } returns "1.2.3"
        every { this@mockk.installer } returns installer
        every { installTime } returns Instant.parse("2026-10-06T10:00:00Z").toKotlinInstant()
      },
      analytics = analytics,
    )
  }

  private companion object {
    const val REMOTE = "https://survey.example/remote"
    const val LOCAL = "https://survey.example/local"
  }
}
