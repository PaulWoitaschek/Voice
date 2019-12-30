package de.ph1b.audiobook.uitools

import android.content.Context
import androidx.annotation.AnyRes
import androidx.annotation.AttrRes

object ThemeUtil {

  @AnyRes
  fun getResourceId(c: Context, @AttrRes attr: Int): Int {
    val ta = c.obtainStyledAttributes(intArrayOf(attr))
    val resId = ta.getResourceId(0, -1)
    ta.recycle()
    require(resId != -1) { "Resource with attr=$attr not found" }
    return resId
  }

}
