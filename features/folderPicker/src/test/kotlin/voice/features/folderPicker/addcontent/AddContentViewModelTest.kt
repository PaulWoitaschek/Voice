package voice.features.folderPicker.addcontent

import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlin.test.Test
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import voice.core.analytics.api.Analytics
import voice.core.common.DispatcherProvider
import voice.navigation.Destination
import voice.navigation.Navigator
import voice.navigation.Origin

class AddContentViewModelTest {

  private val analytics = mockk<Analytics>(relaxUnitFun = true)
  private val navigator = mockk<Navigator>(relaxUnitFun = true)

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
  fun `the survey opens its website`() = runTest {
    viewModel().openHelpSurvey()

    verify { navigator.goTo(Destination.Website(HELP_SURVEY_URL)) }
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
      dispatcherProvider = DispatcherProvider(coroutineContext, coroutineContext, coroutineContext),
      origin = Origin.Onboarding,
    )
}
