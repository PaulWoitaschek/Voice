package voice.features.folderPicker.addcontent

import io.mockk.mockk
import io.mockk.verifyOrder
import kotlin.test.Test
import kotlinx.coroutines.test.runTest
import voice.core.analytics.api.Analytics
import voice.core.common.DispatcherProvider
import voice.navigation.Origin

class AddContentViewModelTest {

  private val analytics = mockk<Analytics>(relaxUnitFun = true)

  @Test
  fun `each answer to why no audiobooks were found is logged`() = runTest {
    val viewModel =
      AddContentViewModel(
        audiobookFolders = mockk(),
        navigator = mockk(),
        audiobookshelfFeatureFlag = mockk(),
        serverAccount = mockk(),
        analytics = analytics,
        dispatcherProvider =
          DispatcherProvider(coroutineContext, coroutineContext, coroutineContext),
        origin = Origin.Onboarding,
      )

    FolderHelpAnswer.entries.forEach(viewModel::onHelpAnswer)

    verifyOrder {
      analytics.event("folder_help_answer", mapOf("answer" to "no_files"))
      analytics.event("folder_help_answer", mapOf("answer" to "not_found"))
      analytics.event("folder_help_answer", mapOf("answer" to "other_app"))
    }
  }
}
