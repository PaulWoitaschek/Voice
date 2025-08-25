package voice.core.common

import android.content.Context
import android.util.TypedValue
import kotlin.math.roundToInt

fun Context.dpToPx(dp: Float) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics)

fun Context.dpToPxRounded(dp: Float): Int = dpToPx(dp).roundToInt()
