package voice.features.support

import android.app.Activity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface SupportBackend {
  val state: StateFlow<SupportBackendState>

  /** What happened to a purchase, e.g. to say thank you. */
  val events: Flow<SupportEvent>

  /** Loads the offers and what's owned. Called whenever the support sheet opens. */
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
