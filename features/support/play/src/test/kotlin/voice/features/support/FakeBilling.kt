package voice.features.support

import android.app.Activity
import kotlinx.coroutines.flow.MutableSharedFlow

class FakeBilling : Billing {

  override val packageName: String = "de.ph1b.audiobook"

  val updates = MutableSharedFlow<PurchaseUpdate>()
  override val purchaseUpdates = updates

  var products: List<BillingProduct>? = SupportProducts.subscriptions.keys.map { BillingProduct(it, "€2") } +
    SupportProducts.tips.map { BillingProduct(it, "€1") }
  var purchases: List<BillingPurchase>? = emptyList()
  var launchSucceeds = true

  val acknowledged = mutableListOf<String>()
  val consumed = mutableListOf<String>()
  val launches = mutableListOf<Pair<String, BillingPurchase?>>()

  override suspend fun products(): List<BillingProduct>? = products

  override suspend fun purchases(): List<BillingPurchase>? = purchases

  override suspend fun acknowledge(purchaseToken: String): Boolean {
    acknowledged += purchaseToken
    return true
  }

  override suspend fun consume(purchaseToken: String): Boolean {
    consumed += purchaseToken
    purchases = purchases?.filterNot { it.purchaseToken == purchaseToken }
    return true
  }

  override fun launch(
    activity: Activity,
    productId: String,
    replace: BillingPurchase?,
  ): Boolean {
    launches += productId to replace
    return launchSucceeds
  }
}
