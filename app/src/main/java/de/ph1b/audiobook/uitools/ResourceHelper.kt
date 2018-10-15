package de.ph1b.audiobook.uitools

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.annotation.AnyRes
import androidx.annotation.AttrRes
import androidx.annotation.CheckResult
import androidx.core.content.ContextCompat

@AnyRes
fun Context.attrToResource(@AttrRes attrId: Int): Int {
  val a = theme.obtainStyledAttributes(intArrayOf(attrId))
  return try {
    val id = a.getResourceId(0, -1)
    check(id != -1) { "Cant resolve id $attrId" }
    id
  } finally {
    a.recycle()
  }
}

@CheckResult
fun Context.drawableFromAttr(@AttrRes attrId: Int): Drawable {
  val res = attrToResource(attrId)
  return ContextCompat.getDrawable(this, res)!!
}
