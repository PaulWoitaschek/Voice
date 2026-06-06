package voice.features.support

import kotlinx.coroutines.flow.StateFlow

interface SupportBackend {
  val state: StateFlow<SupportBackendState>

  fun openSupport()

  fun setSupporterBadgeVisible(visible: Boolean)
}
