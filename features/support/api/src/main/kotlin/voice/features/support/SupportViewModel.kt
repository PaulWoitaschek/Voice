package voice.features.support

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.zacsweers.metro.Inject
import voice.navigation.Navigator

@Inject
class SupportViewModel(
  private val backend: SupportBackend,
  private val navigator: Navigator,
) : SupportListener {

  @Composable
  fun viewState(): SupportViewState {
    val backendState by backend.state.collectAsState()
    return SupportViewState(backendState)
  }

  override fun close() {
    navigator.goBack()
  }

  override fun openSupport() {
    backend.openSupport()
  }

  override fun setSupporterBadgeVisible(visible: Boolean) {
    backend.setSupporterBadgeVisible(visible)
  }
}
