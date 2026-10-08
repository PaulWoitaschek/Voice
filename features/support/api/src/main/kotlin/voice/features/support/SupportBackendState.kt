package voice.features.support

sealed interface SupportBackendState {

  /** Donations go through Ko-fi in the browser. */
  data object KoFi : SupportBackendState

  data object Loading : SupportBackendState

  /** Google Play billing can't be reached, e.g. without the Play Store. */
  data object Unavailable : SupportBackendState

  /** Supporting through Google Play, with subscriptions and one-time tips. */
  data class Play(
    val subscriptions: List<SubscriptionOffer>,
    val tips: List<TipOffer>,
    val activeSubscription: ActiveSubscription?,
  ) : SupportBackendState
}

enum class SupporterTier {
  Tea,
  HoneyTea,
  GoldenMic,
}

enum class SupportPeriod {
  Monthly,
  Yearly,
}

data class SubscriptionOffer(
  val tier: SupporterTier,
  val period: SupportPeriod,
  val formattedPrice: String,
)

data class TipOffer(
  val productId: String,
  val formattedPrice: String,
)

data class ActiveSubscription(
  val tier: SupporterTier,
  val period: SupportPeriod,
)
