package voice.features.support

import android.app.Activity
import app.cash.turbine.test
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import voice.core.analytics.api.Analytics
import voice.core.data.supporter.SupporterBadge
import voice.core.data.supporter.SupporterStatus
import voice.navigation.Destination
import voice.navigation.Navigator
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlaySupportBackendTest {

  private val scope = TestScope()
  private val now = Instant.parse("2026-10-07T10:00:00Z")
  private val billing = FakeBilling()
  private val supporterStatusStore = MemoryDataStore(SupporterStatus())
  private val navigator = mockk<Navigator> {
    every { goTo(any()) } just Runs
  }
  private val activity = mockk<Activity>()

  @Test
  fun `refresh loads the offers`() = scope.runTest {
    val backend = backend()

    backend.refresh()
    runCurrent()

    val state = assertIs<SupportBackendState.Play>(backend.state.value)
    assertEquals(expected = 6, actual = state.subscriptions.size)
    assertEquals(
      expected = SubscriptionOffer(SupporterTier.HoneyTea, SupportPeriod.Yearly, "€2"),
      actual = state.subscriptions.single { it.tier == SupporterTier.HoneyTea && it.period == SupportPeriod.Yearly },
    )
    assertEquals(expected = listOf("tip_small", "tip_medium", "tip_large"), actual = state.tips.map { it.productId })
    assertNull(state.activeSubscription)
  }

  @Test
  fun `refresh without google play is unavailable`() = scope.runTest {
    billing.products = null
    val backend = backend()

    backend.refresh()
    runCurrent()

    assertEquals(expected = SupportBackendState.Unavailable, actual = backend.state.value)
  }

  @Test
  fun `a running subscription is acknowledged and grows the badge`() = scope.runTest {
    val purchaseTime = now.atZone(ZoneOffset.UTC).minusMonths(4).toInstant()
    billing.purchases = listOf(purchase("supporter_honey_tea_yearly", purchaseTime, acknowledged = false))
    val backend = backend()

    backend.refresh()
    runCurrent()

    val state = assertIs<SupportBackendState.Play>(backend.state.value)
    assertEquals(
      expected = ActiveSubscription(SupporterTier.HoneyTea, SupportPeriod.Yearly),
      actual = state.activeSubscription,
    )
    assertEquals(expected = listOf("token_supporter_honey_tea_yearly"), actual = billing.acknowledged)
    val status = supporterStatusStore.data.first()
    assertEquals(expected = 4, actual = status.subscribedMonths)
    assertEquals(expected = purchaseTime.toEpochMilli(), actual = status.supporterSince)
    assertEquals(expected = SupporterBadge.ThreeMonths, actual = status.badge)
  }

  @Test
  fun `an ended subscription keeps the badge`() = scope.runTest {
    supporterStatusStore.updateData {
      SupporterStatus(supporterSince = 1, subscribedSince = 1, subscribedMonths = 7)
    }
    val backend = backend()

    backend.refresh()
    runCurrent()

    assertEquals(
      expected = SupporterStatus(supporterSince = 1, subscribedSince = null, subscribedMonths = 7),
      actual = supporterStatusStore.data.first(),
    )
  }

  @Test
  fun `a tip is consumed and thanked for`() = scope.runTest {
    val backend = backend()
    runCurrent()

    backend.events.test {
      billing.updates.emit(PurchaseUpdate.Purchases(listOf(purchase("tip_small", now))))

      assertEquals(expected = SupportEvent.ThankYou, actual = awaitItem())
    }
    assertEquals(expected = listOf("token_tip_small"), actual = billing.consumed)
    val status = supporterStatusStore.data.first()
    assertTrue(status.tipped)
    assertEquals(expected = SupporterBadge.TipJar, actual = status.badge)
  }

  @Test
  fun `a pending purchase is announced`() = scope.runTest {
    val backend = backend()
    runCurrent()

    backend.events.test {
      billing.updates.emit(
        PurchaseUpdate.Purchases(listOf(purchase("tip_small", now, state = BillingPurchase.State.Pending))),
      )

      assertEquals(expected = SupportEvent.Pending, actual = awaitItem())
    }
    assertEquals(expected = emptyList<String>(), actual = billing.consumed)
  }

  @Test
  fun `a failed purchase is announced`() = scope.runTest {
    val backend = backend()
    runCurrent()

    backend.events.test {
      billing.updates.emit(PurchaseUpdate.Failed)

      assertEquals(expected = SupportEvent.Failed, actual = awaitItem())
    }
  }

  @Test
  fun `switching tiers replaces the running subscription`() = scope.runTest {
    val running = purchase("supporter_tea_monthly", now)
    billing.purchases = listOf(running)
    val backend = backend()
    backend.refresh()
    runCurrent()

    backend.subscribe(activity, SupporterTier.GoldenMic, SupportPeriod.Yearly)

    assertEquals(expected = listOf<Pair<String, BillingPurchase?>>("supporter_golden_mic_yearly" to running), actual = billing.launches)
  }

  @Test
  fun `a purchase screen that doesn't open is announced`() = scope.runTest {
    billing.launchSucceeds = false
    val backend = backend()

    backend.events.test {
      backend.tip(activity, TipOffer("tip_large", "€5"))

      assertEquals(expected = SupportEvent.Failed, actual = awaitItem())
    }
  }

  @Test
  fun `managing opens the running subscription in google play`() = scope.runTest {
    billing.purchases = listOf(purchase("supporter_tea_monthly", now))
    val backend = backend()
    backend.refresh()
    runCurrent()

    backend.manageSubscription()

    verify {
      navigator.goTo(
        Destination.Website(
          "https://play.google.com/store/account/subscriptions?sku=supporter_tea_monthly&package=de.ph1b.audiobook",
        ),
      )
    }
  }

  private fun TestScope.backend(): PlaySupportBackend {
    return PlaySupportBackend(
      billing = billing,
      supporterStatusStore = supporterStatusStore,
      navigator = navigator,
      analytics = mockk<Analytics>(relaxed = true),
      clock = Clock.fixed(now, ZoneOffset.UTC),
      scope = backgroundScope,
    )
  }

  private fun purchase(
    productId: String,
    purchaseTime: Instant,
    state: BillingPurchase.State = BillingPurchase.State.Purchased,
    acknowledged: Boolean = true,
  ) = BillingPurchase(
    productId = productId,
    purchaseToken = "token_$productId",
    purchaseTime = purchaseTime.toEpochMilli(),
    state = state,
    acknowledged = acknowledged,
  )
}
