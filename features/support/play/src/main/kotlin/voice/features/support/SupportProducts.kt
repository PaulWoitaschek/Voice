package voice.features.support

// each tier and period is its own subscription, as a purchase only tells its product and not its base plan
internal object SupportProducts {

  val subscriptions: Map<String, ActiveSubscription> = SupporterTier.entries
    .flatMap { tier -> SupportPeriod.entries.map { period -> ActiveSubscription(tier, period) } }
    .associateBy(::subscriptionId)

  val tips: List<String> = listOf("tip_small", "tip_medium", "tip_large")

  fun subscriptionId(subscription: ActiveSubscription): String {
    return subscriptionId(subscription.tier, subscription.period)
  }

  fun subscriptionId(
    tier: SupporterTier,
    period: SupportPeriod,
  ): String {
    val tierId = when (tier) {
      SupporterTier.Tea -> "tea"
      SupporterTier.HoneyTea -> "honey_tea"
      SupporterTier.GoldenMic -> "golden_mic"
    }
    val periodId = when (period) {
      SupportPeriod.Monthly -> "monthly"
      SupportPeriod.Yearly -> "yearly"
    }
    return "supporter_${tierId}_$periodId"
  }
}
