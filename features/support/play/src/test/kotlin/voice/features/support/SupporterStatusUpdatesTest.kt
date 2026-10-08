package voice.features.support

import voice.core.data.supporter.SupporterBadge
import voice.core.data.supporter.SupporterStatus
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SupporterStatusUpdatesTest {

  private val now = Instant.parse("2026-10-07T10:00:00Z")

  @Test
  fun `a new subscription earns the first cup`() {
    val status = SupporterStatus().withSubscription(activeSince = now, now = now, zone = ZoneOffset.UTC)

    assertEquals(expected = 0, actual = status.subscribedMonths)
    assertEquals(expected = SupporterBadge.FirstCup, actual = status.badge)
    assertEquals(expected = now.toEpochMilli(), actual = status.supporterSince)
  }

  @Test
  fun `a year of subscribing turns golden`() {
    val status = SupporterStatus().withSubscription(
      activeSince = monthsAgo(12),
      now = now,
      zone = ZoneOffset.UTC,
    )

    assertEquals(expected = SupporterBadge.OneYear, actual = status.badge)
  }

  @Test
  fun `switching tiers keeps counting from the first subscription`() {
    val status = SupporterStatus(subscribedSince = monthsAgo(5).toEpochMilli(), subscribedMonths = 5)
      .withSubscription(activeSince = monthsAgo(1), now = now, zone = ZoneOffset.UTC)

    assertEquals(expected = monthsAgo(5).toEpochMilli(), actual = status.subscribedSince)
    assertEquals(expected = 5, actual = status.subscribedMonths)
  }

  @Test
  fun `subscribing again keeps the badge reached before`() {
    val status = SupporterStatus(subscribedMonths = 7)
      .withSubscription(activeSince = monthsAgo(1), now = now, zone = ZoneOffset.UTC)

    assertEquals(expected = 7, actual = status.subscribedMonths)
    assertEquals(expected = SupporterBadge.SixMonths, actual = status.badge)
  }

  @Test
  fun `without a subscription only the running one is forgotten`() {
    val status = SupporterStatus(supporterSince = 1, subscribedSince = 1, subscribedMonths = 3)
      .withSubscription(activeSince = null, now = now, zone = ZoneOffset.UTC)

    assertNull(status.subscribedSince)
    assertEquals(expected = SupporterBadge.ThreeMonths, actual = status.badge)
    assertEquals(expected = 1, actual = status.supporterSince)
  }

  @Test
  fun `a tip earns the tip jar and keeps an earlier start`() {
    assertEquals(
      expected = SupporterStatus(supporterSince = now.toEpochMilli(), tipped = true),
      actual = SupporterStatus().withTip(now),
    )
    assertEquals(
      expected = SupporterStatus(supporterSince = 1, tipped = true),
      actual = SupporterStatus(supporterSince = 1).withTip(now),
    )
  }

  private fun monthsAgo(months: Long): Instant = now.atZone(ZoneOffset.UTC).minusMonths(months).toInstant()
}
