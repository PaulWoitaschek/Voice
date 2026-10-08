package voice.features.support

import android.app.Activity
import kotlinx.coroutines.flow.Flow

/** The parts of Google Play billing that supporting Voice needs, without its hard to fake types. */
interface Billing {

  val packageName: String

  /** What comes back from the purchase screens opened by [launch]. */
  val purchaseUpdates: Flow<PurchaseUpdate>

  /** The products available, with their prices. Null if Google Play can't be reached. */
  suspend fun products(): List<BillingProduct>?

  /** Active subscriptions and tips that weren't consumed yet. Null if Google Play can't be reached. */
  suspend fun purchases(): List<BillingPurchase>?

  suspend fun acknowledge(purchaseToken: String): Boolean

  suspend fun consume(purchaseToken: String): Boolean

  /**
   * Opens the purchase screen for one of the [products]. A subscription can [replace] the one
   * that's running, which switches the tier. Returns false if it can't be opened.
   */
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
