package voice.features.support

import androidx.datastore.core.DataStore
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import voice.core.common.DispatcherProvider
import voice.core.common.MainScope
import voice.core.data.store.SupporterBadgeVisibleStore
import voice.navigation.Destination
import voice.navigation.Navigator

@ContributesBinding(AppScope::class)
class KoFiSupportBackend(
  @SupporterBadgeVisibleStore
  private val supporterBadgeVisibleStore: DataStore<Boolean>,
  private val navigator: Navigator,
  dispatcherProvider: DispatcherProvider,
) : SupportBackend {

  private val scope = MainScope(dispatcherProvider)

  override val state: StateFlow<SupportBackendState> = supporterBadgeVisibleStore.data
    .map { SupportBackendState.Free(supporterBadgeVisible = it) }
    .stateIn(
      scope = scope,
      started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 0),
      initialValue = SupportBackendState.Free(supporterBadgeVisible = false),
    )

  override fun openSupport() {
    navigator.goTo(Destination.Website(KO_FI_URL))
  }

  override fun setSupporterBadgeVisible(visible: Boolean) {
    scope.launch {
      supporterBadgeVisibleStore.updateData { visible }
    }
  }

  private companion object {
    const val KO_FI_URL = "https://ko-fi.com/paul_voice"
  }
}
