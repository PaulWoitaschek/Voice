package voice.features.support

import voice.core.data.supporter.SupporterStatus
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

internal fun SupporterStatus.withTip(at: Instant): SupporterStatus {
  return copy(
    tipped = true,
    supporterSince = minOfNotNull(supporterSince, at.toEpochMilli()),
  )
}

internal fun SupporterStatus.withSubscription(
  activeSince: Instant?,
  now: Instant,
  zone: ZoneId,
): SupporterStatus {
  if (activeSince == null) {
    return copy(subscribedSince = null)
  }
  // switching tiers starts a new purchase, which doesn't restart the count
  val since = minOfNotNull(subscribedSince, activeSince.toEpochMilli())
  val months = ChronoUnit.MONTHS
    .between(Instant.ofEpochMilli(since).atZone(zone), now.atZone(zone))
    .toInt()
    .coerceAtLeast(0)
  return copy(
    supporterSince = minOfNotNull(supporterSince, since),
    subscribedSince = since,
    subscribedMonths = maxOf(subscribedMonths ?: 0, months),
  )
}

private fun minOfNotNull(
  current: Long?,
  new: Long,
): Long = if (current == null) new else minOf(current, new)
