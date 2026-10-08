package voice.features.support

import android.app.Activity
import kotlinx.coroutines.flow.Flow

interface Billing {

  val packageName: String

  val purchaseUpdates: Flow<PurchaseUpdate>

  suspend fun products(): List<BillingProduct>?

  suspend fun purchases(): List<BillingPurchase>?

  suspend fun acknowledge(purchaseToken: String): Boolean

  suspend fun consume(purchaseToken: String): Boolean

  fun launch(
    activity: Activity,
    productId: String,
    replace: BillingPurchase?,
  ): Boolean
}

data class BillingProduct(
  val productId: String,
  val formattedPrice: String,
)

data class BillingPurchase(
  val productId: String,
  val purchaseToken: String,
  val purchaseTime: Long,
  val state: State,
  val acknowledged: Boolean,
) {

  enum class State {
    Purchased,
    Pending,
  }
}

sealed interface PurchaseUpdate {
  data class Purchases(val purchases: List<BillingPurchase>) : PurchaseUpdate
  data object Canceled : PurchaseUpdate
  data object Failed : PurchaseUpdate
}
