package voice.features.support

sealed interface SupportBackendState {

  data object KoFi : SupportBackendState

  data object Loading : SupportBackendState

  data object Unavailable : SupportBackendState

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
