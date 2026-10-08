package voice.features.support

import voice.core.data.supporter.SupporterBadge
import java.time.YearMonth

data class SupportViewState(
  val content: Content,
  /** The badge earned so far, null for someone who never supported. */
  val badge: SupporterBadge?,
  val supporterSince: YearMonth?,
  /** Shown right after a payment went through, instead of the [content]. */
  val thankYou: Boolean,
  val message: Message?,
) {

  sealed interface Content {

    data object KoFi : Content

    data object Loading : Content

    data object Unavailable : Content

    data class Play(
      /** Null when no subscriptions are offered, which leaves the tips. */
      val subscription: Subscription?,
      val tips: List<TipOffer>,
      val showTips: Boolean,
      /** A note from the developer, e.g. what the support paid for this month. */
      val note: String?,
    ) : Content
  }

  data class Subscription(
    val period: SupportPeriod,
    /** The tiers for the [period], cheapest first. */
    val tiers: List<Tier>,
    val selectedTier: SupporterTier,
    val action: Action,
    /** Whether a subscription is running, maybe with another tier or period than the selected one. */
    val subscribed: Boolean,
  )

  data class Tier(
    val tier: SupporterTier,
    val formattedPrice: String,
    /** Whether this is the running subscription. */
    val active: Boolean,
  )

  sealed interface Action {
    data class Subscribe(
      val formattedPrice: String,
      val period: SupportPeriod,
    ) : Action

    /** Changes the running subscription to the selected tier and period. */
    data class Switch(
      val formattedPrice: String,
      val period: SupportPeriod,
    ) : Action

    /** The selected tier is the running subscription. */
    data object Manage : Action
  }

  enum class Message {
    Pending,
    Failed,
  }

  companion object {
    fun preview(content: Content = previewPlayContent()) = SupportViewState(
      content = content,
      badge = SupporterBadge.ThreeMonths,
      supporterSince = YearMonth.of(2026, 3),
      thankYou = false,
      message = null,
    )

    fun previewPlayContent() = Content.Play(
      subscription = Subscription(
        period = SupportPeriod.Monthly,
        tiers = listOf(
          Tier(SupporterTier.Tea, "€0.99", active = false),
          Tier(SupporterTier.HoneyTea, "€2.99", active = true),
          Tier(SupporterTier.GoldenMic, "€5.99", active = false),
        ),
        selectedTier = SupporterTier.GoldenMic,
        action = Action.Switch("€5.99", SupportPeriod.Monthly),
        subscribed = true,
      ),
      tips = listOf(
        TipOffer("tip_small", "€2.99"),
        TipOffer("tip_medium", "€4.99"),
        TipOffer("tip_large", "€9.99"),
      ),
      showTips = false,
      note = "This month your tea paid for Android 17 testing and the new sleep timer.",
    )
  }
}
