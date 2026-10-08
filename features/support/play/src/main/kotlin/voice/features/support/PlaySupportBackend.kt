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
          val subscriptions = purchased.filter { it.productId in SupportProducts.subscriptions }
          // set right away, so a failing reload can't lead to a second subscription instead of a switch
          if (subscriptions.isNotEmpty()) {
            updateSubscription(subscriptions)
            (_state.value as? SupportBackendState.Play)?.let { state ->
              _state.value = state.copy(activeSubscription = activeSubscription())
            }
          }
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
        // e.g. it was owned already
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
      updateSubscription(purchased.filter { it.productId in SupportProducts.subscriptions })
      _state.value = SupportBackendState.Play(
        subscriptions = products.mapNotNull { product ->
          val offer = SupportProducts.subscriptions[product.productId] ?: return@mapNotNull null
          SubscriptionOffer(tier = offer.tier, period = offer.period, formattedPrice = product.formattedPrice)
        },
        tips = SupportProducts.tips.mapNotNull { productId ->
          val product = products.find { it.productId == productId } ?: return@mapNotNull null
          TipOffer(productId = productId, formattedPrice = product.formattedPrice)
        },
        activeSubscription = activeSubscription(),
      )
    }
  }

  private suspend fun updateSubscription(subscriptions: List<BillingPurchase>) {
    // after switching tiers, the newest one is the one running
    activePurchase = subscriptions.maxByOrNull { it.purchaseTime }
    supporterStatusStore.updateData { status ->
      status.withSubscription(
        activeSince = subscriptions.minOfOrNull { it.purchaseTime }?.let(Instant::ofEpochMilli),
        now = clock.instant(),
        zone = clock.zone,
      )
    }
  }

  private fun activeSubscription(): ActiveSubscription? {
    return activePurchase?.let { SupportProducts.subscriptions[it.productId] }
  }

  // Google Play refunds subscriptions that aren't acknowledged within three days
  private suspend fun handle(purchased: List<BillingPurchase>) {
    purchased.forEach { purchase ->
      when (purchase.productId) {
        in SupportProducts.subscriptions -> {
          if (!purchase.acknowledged && !billing.acknowledge(purchase.purchaseToken)) {
            Logger.w("Acknowledging ${purchase.productId} failed")
          }
        }
        in SupportProducts.tips -> {
          // credited first, as a consumed tip is gone even when the result doesn't make it back
          supporterStatusStore.updateData { it.withTip(Instant.ofEpochMilli(purchase.purchaseTime)) }
          if (!billing.consume(purchase.purchaseToken)) {
            Logger.w("Consuming ${purchase.productId} failed")
          }
        }
      }
    }
  }

  private companion object {
    const val MANAGE_SUBSCRIPTIONS_URL = "https://play.google.com/store/account/subscriptions"
  }
}
