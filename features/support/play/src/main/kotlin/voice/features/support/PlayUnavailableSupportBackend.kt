package voice.features.support

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@ContributesBinding(AppScope::class)
@Inject
class PlayUnavailableSupportBackend : SupportBackend {

  override val state: StateFlow<SupportBackendState> = MutableStateFlow(SupportBackendState.PlayUnavailable)

  override fun openSupport() {
  }
}
