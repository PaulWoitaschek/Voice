package voice.features.settings.developer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import voice.core.common.DispatcherProvider
import voice.core.common.MainScope
import voice.core.remoteconfig.api.FmcTokenProvider
import voice.core.remoteconfig.api.RemoteConfig
import voice.navigation.Navigator

@Inject
class DeveloperSettingsViewModel(
  private val navigator: Navigator,
  private val fmcTokenProvider: FmcTokenProvider,
  private val remoteConfig: RemoteConfig,
  dispatcherProvider: DispatcherProvider,
) {

  private val scope = MainScope(dispatcherProvider)

  @Composable
  fun viewState(): DeveloperSettingsViewState {
    val fcmToken: String? by produceState(null) {
      value = fmcTokenProvider.token()
    }
    return DeveloperSettingsViewState(fcmToken = fcmToken)
  }

  private var refreshJob: Job? = null
  fun refreshRemoteConfig() {
    if (refreshJob?.isActive == true) return
    refreshJob = scope.launch {
      remoteConfig.refresh()
    }
  }

  fun close() {
    navigator.goBack()
  }
}
