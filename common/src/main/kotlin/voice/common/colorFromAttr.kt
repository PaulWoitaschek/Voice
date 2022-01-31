package voice.common

import android.content.Context
import androidx.annotation.AnyRes
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt

@ColorInt
fun Context.colorFromAttr(@AttrRes id: Int): Int {
  val colorRes = getResourceId(this, id)
  return getColor(colorRes)
}

@AnyRes
private fun getResourceId(context: Context, @AttrRes attr: Int): Int {
  val ta = context.obtainStyledAttributes(intArrayOf(attr))
  val resId = ta.getResourceId(0, -1)
  ta.recycle()
  require(resId != -1) { "Resource with attr=$attr not found" }
  return resId
}
