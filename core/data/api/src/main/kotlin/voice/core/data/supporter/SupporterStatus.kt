package voice.core.data.supporter

import kotlinx.serialization.Serializable

/**
 * How someone supported Voice. It's a thank-you only and unlocks nothing. What was earned is kept
 * after a subscription ends.
 */
@Serializable
public data class SupporterStatus(
  /** When the support started, in epoch millis. Null for someone who never supported. */
  val supporterSince: Long? = null,
  /** When the running subscription started, in epoch millis. Null without one. */
  val subscribedSince: Long? = null,
  /** The most months in a row of a subscription. Null if there never was one. */
  val subscribedMonths: Int? = null,
  val tipped: Boolean = false,
) {

  val badge: SupporterBadge?
    get() {
      val months = subscribedMonths
      return when {
        months == null -> if (tipped) SupporterBadge.TipJar else null
        months >= 12 -> SupporterBadge.OneYear
        months >= 6 -> SupporterBadge.SixMonths
        months >= 3 -> SupporterBadge.ThreeMonths
        else -> SupporterBadge.FirstCup
      }
    }
}

/** The badge grows with every month of a subscription. A one-time tip gets the tip jar. */
public enum class SupporterBadge {
  TipJar,
  FirstCup,
  ThreeMonths,
  SixMonths,
  OneYear,
  ;

  public companion object {
    /** The badges a subscription grows through, in order. */
    public val growth: List<SupporterBadge> = listOf(FirstCup, ThreeMonths, SixMonths, OneYear)
  }
}
