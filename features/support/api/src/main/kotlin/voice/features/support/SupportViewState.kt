package voice.features.support

import voice.core.data.supporter.SupporterBadge
import java.time.YearMonth

data class SupportViewState(
  val content: Content,
  val badge: SupporterBadge?,
  val supporterSince: YearMonth?,
  val thankYou: Boolean,
  val message: Message?,
) {

  sealed interface Content {

    data object KoFi : Content

    data object Loading : Content

    data object Unavailable : Content

    data class Play(
      val subscription: Subscription?,
      val tips: List<TipOffer>,
      val showTips: Boolean,
      val note: String?,
    ) : Content
  }

  data class Subscription(
    val period: SupportPeriod,
    val tiers: List<Tier>,
    val selectedTier: SupporterTier,
    val action: Action,
    val subscribed: Boolean,
  )

  data class Tier(
    val tier: SupporterTier,
    val formattedPrice: String,
    val active: Boolean,
  )

  sealed interface Action {
    data class Subscribe(
      val formattedPrice: String,
      val period: SupportPeriod,
    ) : Action

    data class Switch(
      val formattedPrice: String,
      val period: SupportPeriod,
    ) : Action

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
