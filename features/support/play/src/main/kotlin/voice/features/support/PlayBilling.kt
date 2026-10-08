package voice.features.support

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingFlowParams.ProductDetailsParams.SubscriptionProductReplacementParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.consumePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import voice.core.logging.api.Logger
import kotlin.coroutines.resume

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class PlayBilling(context: Context) : Billing {

  override val packageName: String = context.packageName

  private val updates = MutableSharedFlow<PurchaseUpdate>(extraBufferCapacity = 8)
  override val purchaseUpdates: Flow<PurchaseUpdate> = updates

  private val client = BillingClient.newBuilder(context)
    .setListener { result, purchases ->
      updates.tryEmit(
        when (result.responseCode) {
          BillingResponseCode.OK -> PurchaseUpdate.Purchases(purchases.orEmpty().mapNotNull { it.toBillingPurchase() })
          BillingResponseCode.USER_CANCELED -> PurchaseUpdate.Canceled
          else -> {
            Logger.w("Purchase failed with ${result.responseCode}: ${result.debugMessage}")
            PurchaseUpdate.Failed
          }
        },
      )
    }
    .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
    .enableAutoServiceReconnection()
    .build()

  private val connectMutex = Mutex()
  private var connected = false
  private val productDetails = mutableMapOf<String, ProductDetails>()

  override suspend fun products(): List<BillingProduct>? {
    if (!connect()) return null
    val subscriptions = queryProductDetails(ProductType.SUBS, SupportProducts.subscriptions.keys) ?: return null
    val tips = queryProductDetails(ProductType.INAPP, SupportProducts.tips) ?: return null
    return (subscriptions + tips).mapNotNull { details ->
      productDetails[details.productId] = details
      val price = details.baseSubscriptionOffer()?.pricingPhases?.pricingPhaseList?.lastOrNull()?.formattedPrice
        ?: details.oneTimePurchaseOfferDetails?.formattedPrice
        ?: return@mapNotNull null
      BillingProduct(productId = details.productId, formattedPrice = price)
    }
  }

  private suspend fun queryProductDetails(
    type: String,
    productIds: Collection<String>,
  ): List<ProductDetails>? {
    val params = QueryProductDetailsParams.newBuilder()
      .setProductList(
        productIds.map { productId ->
          QueryProductDetailsParams.Product.newBuilder()
            .setProductId(productId)
            .setProductType(type)
            .build()
        },
      )
      .build()
    val result = client.queryProductDetails(params)
    if (result.billingResult.responseCode != BillingResponseCode.OK) {
      Logger.w("Querying $type products failed: ${result.billingResult.debugMessage}")
      return null
    }
    return result.productDetailsList.orEmpty()
  }

  override suspend fun purchases(): List<BillingPurchase>? {
    if (!connect()) return null
    return listOf(ProductType.SUBS, ProductType.INAPP).flatMap { type ->
      val result = client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(type).build())
      if (result.billingResult.responseCode != BillingResponseCode.OK) {
        Logger.w("Querying $type purchases failed: ${result.billingResult.debugMessage}")
        return null
      }
      result.purchasesList.mapNotNull { it.toBillingPurchase() }
    }
  }

  override suspend fun acknowledge(purchaseToken: String): Boolean {
    if (!connect()) return false
    val params = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchaseToken).build()
    return client.acknowledgePurchase(params).isOk()
  }

  override suspend fun consume(purchaseToken: String): Boolean {
    if (!connect()) return false
    val params = ConsumeParams.newBuilder().setPurchaseToken(purchaseToken).build()
    return client.consumePurchase(params).billingResult.isOk()
  }

  override fun launch(
    activity: Activity,
    productId: String,
    replace: BillingPurchase?,
  ): Boolean {
    val details = productDetails[productId] ?: return false
    val offerToken = details.baseSubscriptionOffer()?.offerToken
      ?: details.oneTimePurchaseOfferDetails?.offerToken
    val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
      .setProductDetails(details)
      .apply {
        if (offerToken != null) setOfferToken(offerToken)
        if (replace != null) {
          setSubscriptionProductReplacementParams(
            SubscriptionProductReplacementParams.newBuilder()
              .setOldProductId(replace.productId)
              // the time left on the running subscription is credited towards the new one
              .setReplacementMode(SubscriptionProductReplacementParams.ReplacementMode.WITH_TIME_PRORATION)
              .build(),
          )
        }
      }
      .build()
    val flowParams = BillingFlowParams.newBuilder()
      .setProductDetailsParamsList(listOf(productParams))
      .apply {
        if (replace != null) {
          setSubscriptionUpdateParams(
            BillingFlowParams.SubscriptionUpdateParams.newBuilder()
              .setOldPurchaseToken(replace.purchaseToken)
              .build(),
          )
        }
      }
      .build()
    val result = client.launchBillingFlow(activity, flowParams)
    if (!result.isOk()) {
      Logger.w("Launching the purchase of $productId failed: ${result.debugMessage}")
    }
    return result.isOk()
  }

  /**
   * Connects once. After that the client reconnects on its own when it's used, and connecting again
   * while it does would fail.
   */
  private suspend fun connect(): Boolean = connectMutex.withLock {
    if (connected) return@withLock true
    connected = suspendCancellableCoroutine { continuation ->
      client.startConnection(
        object : BillingClientStateListener {
          override fun onBillingSetupFinished(result: BillingResult) {
            if (!result.isOk()) {
              Logger.w("Billing setup failed: ${result.debugMessage}")
            }
            if (continuation.isActive) continuation.resume(result.isOk())
          }

          override fun onBillingServiceDisconnected() {
            if (continuation.isActive) continuation.resume(false)
          }
        },
      )
    }
    connected
  }
}

/** The plain base plan of a subscription, without any special offers on top. */
private fun ProductDetails.baseSubscriptionOffer(): ProductDetails.SubscriptionOfferDetails? {
  return subscriptionOfferDetails?.firstOrNull { it.offerId == null }
}

private fun BillingResult.isOk(): Boolean = responseCode == BillingResponseCode.OK

private fun Purchase.toBillingPurchase(): BillingPurchase? {
  return BillingPurchase(
    productId = products.firstOrNull() ?: return null,
    purchaseToken = purchaseToken,
    purchaseTime = purchaseTime,
    state = when (purchaseState) {
      Purchase.PurchaseState.PURCHASED -> BillingPurchase.State.Purchased
      Purchase.PurchaseState.PENDING -> BillingPurchase.State.Pending
      else -> return null
    },
    acknowledged = isAcknowledged,
  )
}
