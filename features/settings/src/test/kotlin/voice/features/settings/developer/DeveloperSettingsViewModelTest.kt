package voice.features.settings.developer

import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import app.cash.turbine.test
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Test
import voice.core.common.DispatcherProvider
import voice.core.featureflag.MemoryFeatureFlag
import voice.core.remoteconfig.api.FmcTokenProvider
import voice.core.remoteconfig.api.RemoteConfig
import voice.navigation.Navigator

class DeveloperSettingsViewModelTest {

  private val scope = TestScope()
  private val fmcTokenProvider = mockk<FmcTokenProvider> {
    coEvery { token() } returns "fcm-token"
  }
  private val remoteConfig = mockk<RemoteConfig>(relaxed = true)
  private val booleanFlag = MemoryFeatureFlag(
    initialValue = false,
    key = "boolean_flag",
    description = "Boolean feature flag description",
  )
  private val stringFlag = MemoryFeatureFlag(
    initialValue = "Mozilla/5.0",
    key = "string_flag",
    description = "String feature flag description",
  )
  private val viewModel = DeveloperSettingsViewModel(
    navigator = Navigator(),
    fmcTokenProvider = fmcTokenProvider,
    remoteConfig = remoteConfig,
    dispatcherProvider = DispatcherProvider(scope.coroutineContext, scope.coroutineContext, scope.coroutineContext),
    featureFlags = setOf(stringFlag, booleanFlag),
  )

  @Test
  fun `view state includes feature flag descriptions`() = scope.runTest {
    backgroundScope.launchMolecule(RecompositionMode.Immediate) {
      viewModel.viewState()
    }.test {
      awaitItem() shouldBe DeveloperSettingsViewState(
        fcmToken = "fcm-token",
        featureFlags = listOf(
          DeveloperSettingsViewState.FeatureFlagViewState.BooleanFlag(
            key = "boolean_flag",
            description = "Boolean feature flag description",
            value = false,
            isOverridden = false,
          ),
          DeveloperSettingsViewState.FeatureFlagViewState.StringFlag(
            key = "string_flag",
            description = "String feature flag description",
            value = "Mozilla/5.0",
            isOverridden = false,
          ),
        ),
      )
    }
  }
}
