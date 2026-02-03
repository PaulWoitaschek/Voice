package voice.features.settings.developer

import androidx.compose.runtime.Composable
import dev.zacsweers.metro.Inject
import voice.navigation.Navigator

@Inject
class DeveloperSettingsViewModel(private val navigator: Navigator) {

  @Composable
  fun viewState(): DeveloperSettingsViewState {
    return DeveloperSettingsViewState("Unicorn")
  }

  fun close() {
    navigator.goBack()
  }
}
