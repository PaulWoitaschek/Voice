package voice.features.support

import android.app.Activity

interface SupportListener {
  fun close()

  fun openKoFi()

  fun selectFrequency(frequency: SupportFrequency)

  fun selectTier(tier: SupporterTier)

  fun confirm(activity: Activity)

  fun manageSubscription()

  fun retry()

  companion object {
    fun noop() = object : SupportListener {
      override fun close() {}
      override fun openKoFi() {}
      override fun selectFrequency(frequency: SupportFrequency) {}
      override fun selectTier(tier: SupporterTier) {}
      override fun confirm(activity: Activity) {}
      override fun manageSubscription() {}
      override fun retry() {}
    }
  }
}
