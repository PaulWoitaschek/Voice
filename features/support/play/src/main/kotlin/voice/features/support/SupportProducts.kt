package voice.features.support

// each tier and period is its own subscription, as a purchase only tells its product and not its base plan
internal object SupportProducts {

  val subscriptions: Map<String, ActiveSubscription> = SupporterTier.entries
    .flatMap { tier -> SupportPeriod.entries.map { period -> ActiveSubscription(tier, period) } }
    .associateBy(::subscriptionId)

  val tips: Map<String, SupporterTier> = SupporterTier.entries.associateBy(::tipId)

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

  fun tipId(tier: SupporterTier): String {
    return when (tier) {
      SupporterTier.Tea -> "tip_small"
      SupporterTier.HoneyTea -> "tip_medium"
      SupporterTier.GoldenMic -> "tip_large"
    }
  }
}
