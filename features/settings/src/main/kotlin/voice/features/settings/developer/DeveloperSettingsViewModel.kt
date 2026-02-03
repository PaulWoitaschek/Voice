package voice.features.settings.developer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import dev.zacsweers.metro.Inject
import voice.core.remoteconfig.api.FmcTokenProvider
import voice.navigation.Navigator

@Inject
class DeveloperSettingsViewModel(
  private val navigator: Navigator,
  private val fmcTokenProvider: FmcTokenProvider,
) {

  @Composable
  fun viewState(): DeveloperSettingsViewState {
    val fcmToken: String? by produceState(null) {
      value = fmcTokenProvider.token()
    }
    return DeveloperSettingsViewState(fcmToken = fcmToken)
  }

  fun close() {
    navigator.goBack()
  }
}
