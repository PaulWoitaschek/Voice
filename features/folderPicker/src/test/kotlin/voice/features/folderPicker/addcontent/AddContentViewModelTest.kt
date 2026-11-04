package voice.features.folderPicker.addcontent

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import voice.core.analytics.api.Analytics
import voice.core.common.DispatcherProvider
import voice.core.featureflag.MemoryFeatureFlag
import voice.navigation.Destination
import voice.navigation.Navigator
import voice.navigation.Origin

class AddContentViewModelTest {

  private val analytics = mockk<Analytics>(relaxUnitFun = true)
  private val navigator = mockk<Navigator>(relaxUnitFun = true)
  private val surveyUrl = MemoryFeatureFlag("")

  @Test
  fun `each answer to why no audiobooks were found is logged`() = runTest {
    val viewModel = viewModel()

    FolderHelpAnswer.entries.forEach(viewModel::onHelpAnswer)

    verifyOrder {
      analytics.event("folder_help_answer", mapOf("answer" to "no_files"))
      analytics.event("folder_help_answer", mapOf("answer" to "not_found"))
      analytics.event("folder_help_answer", mapOf("answer" to "other_app"))
    }
  }

  @Test
  fun `without a survey url there is no survey`() = runTest {
    assertEquals(expected = false, actual = viewModel().canOpenHelpSurvey)
  }

  @Test
  fun `the survey opens with the answer and the app version`() = runTest {
    surveyUrl.overwrite("https://tally.so/r/abc")
    val viewModel = viewModel()

    assertEquals(expected = true, actual = viewModel.canOpenHelpSurvey)
    viewModel.openHelpSurvey(FolderHelpAnswer.NotFound)

    verify {
      navigator.goTo(
        Destination.Website("https://tally.so/r/abc?answer=not_found&app_version=26.11.0"),
      )
    }
  }

  @Test
  fun `the survey opens without an answer before one is picked`() = runTest {
    surveyUrl.overwrite("https://tally.so/r/abc?ref=voice")

    viewModel().openHelpSurvey(answer = null)

    verify {
      navigator.goTo(Destination.Website("https://tally.so/r/abc?ref=voice&app_version=26.11.0"))
    }
  }

  @Test
  fun `LibriVox opens its website`() = runTest {
    viewModel().openLibriVox()

    verify { navigator.goTo(Destination.Website("https://librivox.org")) }
  }

  private fun TestScope.viewModel() =
    AddContentViewModel(
      audiobookFolders = mockk(),
      navigator = navigator,
      audiobookshelfFeatureFlag = mockk(),
      serverAccount = mockk(),
      analytics = analytics,
      helpSurveyUrlFeatureFlag = FolderHelpSurveyUrlFeatureFlag(surveyUrl),
      appInfoProvider = mockk { every { versionName } returns "26.11.0" },
      dispatcherProvider = DispatcherProvider(coroutineContext, coroutineContext, coroutineContext),
      origin = Origin.Onboarding,
    )
}
