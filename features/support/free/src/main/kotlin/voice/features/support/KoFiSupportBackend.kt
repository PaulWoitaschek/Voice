package voice.features.support

import android.app.Activity
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import voice.navigation.Destination
import voice.navigation.Navigator

@ContributesBinding(AppScope::class)
class KoFiSupportBackend(private val navigator: Navigator) : SupportBackend {

  override val state: StateFlow<SupportBackendState> = MutableStateFlow(SupportBackendState.KoFi)

  override val events: Flow<SupportEvent> = emptyFlow()

  override fun refresh() {}

  override fun openKoFi() {
    navigator.goTo(Destination.Website(KO_FI_URL))
  }

  override fun subscribe(
    activity: Activity,
    tier: SupporterTier,
    period: SupportPeriod,
  ) {}

  override fun tip(
    activity: Activity,
    tip: TipOffer,
  ) {}

  override fun manageSubscription() {}

  private companion object {
    const val KO_FI_URL = "https://ko-fi.com/paul_voice"
  }
}
