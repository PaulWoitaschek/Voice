package voice.features.support

import android.app.Activity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface SupportBackend {
  val state: StateFlow<SupportBackendState>

  val events: Flow<SupportEvent>

  fun refresh()

  fun openKoFi()

  fun subscribe(
    activity: Activity,
    tier: SupporterTier,
    period: SupportPeriod,
  )

  fun tip(
    activity: Activity,
    tip: TipOffer,
  )

  fun manageSubscription()
}
