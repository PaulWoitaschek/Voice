package de.ph1b.audiobook.uitools

import android.content.Context
import android.graphics.drawable.Drawable
import android.support.annotation.AnyRes
import android.support.annotation.AttrRes
import android.support.annotation.CheckResult
import android.support.v4.content.ContextCompat


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
