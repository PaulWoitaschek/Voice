package voice.common

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.os.Parcelable
import android.util.TypedValue
import android.view.LayoutInflater
import kotlin.math.roundToInt

fun Context.layoutInflater(): LayoutInflater = LayoutInflater.from(this)

fun Context.dpToPx(dp: Float) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics)

fun Context.dpToPxRounded(dp: Float): Int = dpToPx(dp).roundToInt()

fun checkMainThread() {
  check(Looper.getMainLooper() == Looper.myLooper()) {
    "Is not on ui thread!"
  }
}

inline fun <reified T : Parcelable> Bundle.parcelable(key: String): T? {
  return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    getParcelable(key, T::class.java)
  } else {
    @Suppress("DEPRECATION")
    getParcelable(key)
  }
}
