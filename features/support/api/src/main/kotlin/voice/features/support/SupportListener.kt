package voice.features.support

import android.app.Activity

interface SupportListener {
  fun close()

  fun openKoFi()

  fun selectPeriod(period: SupportPeriod)

  fun selectTier(tier: SupporterTier)

  /** Subscribes to, switches to, or manages the selected tier, depending on the [SupportViewState.Action]. */
  fun confirm(activity: Activity)

  fun toggleTips()

  fun tip(
    activity: Activity,
    tip: TipOffer,
  )

  fun manageSubscription()

  fun retry()

  companion object {
    fun noop() = object : SupportListener {
      override fun close() {}
      override fun openKoFi() {}
      override fun selectPeriod(period: SupportPeriod) {}
      override fun selectTier(tier: SupporterTier) {}
      override fun confirm(activity: Activity) {}
      override fun toggleTips() {}
      override fun tip(
        activity: Activity,
        tip: TipOffer,
      ) {}

      override fun manageSubscription() {}
      override fun retry() {}
    }
  }
}
