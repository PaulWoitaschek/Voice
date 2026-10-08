package voice.features.support

/**
 * The products set up in the Play Console. Each tier and period is a subscription of its own, as a
 * purchase only tells its product and not its base plan.
 *
 * `scripts/play_products.main.kts` creates them on Google Play from `fastlane/play_products.json`.
 */
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
