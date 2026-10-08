package voice.core.data.supporter

import kotlinx.serialization.Serializable

@Serializable
public data class SupporterStatus(
  val supporterSince: Long? = null,
  val subscribedSince: Long? = null,
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

public enum class SupporterBadge {
  TipJar,
  FirstCup,
  ThreeMonths,
  SixMonths,
  OneYear,
  ;

  public companion object {
    public val growth: List<SupporterBadge> = listOf(FirstCup, ThreeMonths, SixMonths, OneYear)
  }
}
