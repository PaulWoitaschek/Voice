package voice.features.support

import android.app.Activity
import androidx.datastore.core.DataStore
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import voice.core.analytics.api.Analytics
import voice.core.data.store.SupporterStatusStore
import voice.core.data.supporter.SupporterStatus
import voice.core.logging.api.Logger
import voice.navigation.Destination
import voice.navigation.Navigator
import java.time.Clock
import java.time.Instant

/**
 * Supporting Voice through Google Play. Subscriptions are acknowledged and tips consumed, so a tip
 * can be given again. Both only earn a badge, which is kept in the [SupporterStatus].
 */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class PlaySupportBackend(
  private val billing: Billing,
  @SupporterStatusStore
  private val supporterStatusStore: DataStore<SupporterStatus>,
  private val navigator: Navigator,
  private val analytics: Analytics,
  private val clock: Clock,
  private val scope: CoroutineScope,
) : SupportBackend {

  private val _state = MutableStateFlow<SupportBackendState>(SupportBackendState.Loading)
  override val state: StateFlow<SupportBackendState> = _state.asStateFlow()

  private val _events = MutableSharedFlow<SupportEvent>(extraBufferCapacity = 8)
  override val events: Flow<SupportEvent> = _events

  private val loadMutex = Mutex()
  private var activePurchase: BillingPurchase? = null

  init {
    scope.launch {
      billing.purchaseUpdates.collect(::onPurchaseUpdate)
    }
  }

  override fun refresh() {
    scope.launch { load() }
  }

  override fun openKoFi() {}

  override fun subscribe(
    activity: Activity,
    tier: SupporterTier,
    period: SupportPeriod,
  ) {
    analytics.event(
      "support_subscribe",
      mapOf(
        "tier" to tier.name,
        "period" to period.name,
        "switch" to (activePurchase != null).toString(),
      ),
    )
    launch(activity, SupportProducts.subscriptionId(tier, period), replace = activePurchase)
  }

  override fun tip(
    activity: Activity,
    tip: TipOffer,
  ) {
    analytics.event("support_tip", mapOf("product" to tip.productId))
    launch(activity, tip.productId, replace = null)
  }

  private fun launch(
    activity: Activity,
    productId: String,
    replace: BillingPurchase?,
  ) {
    if (!billing.launch(activity, productId, replace)) {
      _events.tryEmit(SupportEvent.Failed)
    }
  }

  override fun manageSubscription() {
    val productId = activePurchase?.productId
    val url = if (productId == null) {
      MANAGE_SUBSCRIPTIONS_URL
    } else {
      "$MANAGE_SUBSCRIPTIONS_URL?sku=$productId&package=${billing.packageName}"
    }
    navigator.goTo(Destination.Website(url))
  }

  private suspend fun onPurchaseUpdate(update: PurchaseUpdate) {
    when (update) {
      is PurchaseUpdate.Purchases -> {
        val purchased = update.purchases.filter { it.state == BillingPurchase.State.Purchased }
        if (purchased.isNotEmpty()) {
          handle(purchased)
          load()
          purchased.forEach {
            analytics.event("support_purchased", mapOf("product" to it.productId))
          }
          _events.emit(SupportEvent.ThankYou)
        } else if (update.purchases.isNotEmpty()) {
          _events.emit(SupportEvent.Pending)
        }
      }
      PurchaseUpdate.Canceled -> {}
      PurchaseUpdate.Failed -> {
        analytics.event("support_purchase_failed")
        _events.emit(SupportEvent.Failed)
        // e.g. it was owned already, which a fresh look at the purchases sorts out
        load()
      }
    }
  }

  private suspend fun load() {
    loadMutex.withLock {
      if (_state.value !is SupportBackendState.Play) {
        _state.value = SupportBackendState.Loading
      }
      val products = billing.products()
      val purchases = billing.purchases()
      if (products == null || purchases == null) {
        if (_state.value !is SupportBackendState.Play) {
          _state.value = SupportBackendState.Unavailable
        }
        return
      }
      val purchased = purchases.filter { it.state == BillingPurchase.State.Purchased }
      handle(purchased)
      val subscriptions = purchased.filter { it.productId in SupportProducts.subscriptions }
      // after switching tiers, the newest one is the one running
      val subscription = subscriptions.maxByOrNull { it.purchaseTime }
      activePurchase = subscription
      supporterStatusStore.updateData { status ->
        status.withSubscription(
          activeSince = subscriptions.minOfOrNull { it.purchaseTime }?.let(Instant::ofEpochMilli),
          now = clock.instant(),
          zone = clock.zone,
        )
      }
      _state.value = SupportBackendState.Play(
        subscriptions = products.mapNotNull { product ->
          val offer = SupportProducts.subscriptions[product.productId] ?: return@mapNotNull null
          SubscriptionOffer(tier = offer.tier, period = offer.period, formattedPrice = product.formattedPrice)
        },
        tips = SupportProducts.tips.mapNotNull { productId ->
          val product = products.find { it.productId == productId } ?: return@mapNotNull null
          TipOffer(productId = productId, formattedPrice = product.formattedPrice)
        },
        activeSubscription = subscription?.let { SupportProducts.subscriptions[it.productId] },
      )
    }
  }

  /** Subscriptions must be acknowledged, or Google Play refunds them after three days. */
  private suspend fun handle(purchased: List<BillingPurchase>) {
    purchased.forEach { purchase ->
      when (purchase.productId) {
        in SupportProducts.subscriptions -> {
          // a failed one is tried again on the next refresh
          if (!purchase.acknowledged && !billing.acknowledge(purchase.purchaseToken)) {
            Logger.w("Acknowledging ${purchase.productId} failed")
          }
        }
        in SupportProducts.tips -> {
          // consuming also acknowledges it and lets the same tip be given again
          if (billing.consume(purchase.purchaseToken)) {
            supporterStatusStore.updateData { it.withTip(Instant.ofEpochMilli(purchase.purchaseTime)) }
          }
        }
      }
    }
  }

  private companion object {
    const val MANAGE_SUBSCRIPTIONS_URL = "https://play.google.com/store/account/subscriptions"
  }
}
