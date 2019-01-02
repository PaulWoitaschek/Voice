package de.ph1b.audiobook.features.gridCount

import android.content.Context
import de.ph1b.audiobook.misc.dpToPx
import kotlin.math.roundToInt

fun gridColumnCount(context: Context): Int {
  val displayMetrics = context.resources.displayMetrics
  val widthPx = displayMetrics.widthPixels.toFloat()
  val desiredPx = context.dpToPx(180F)
  val columns = (widthPx / desiredPx).roundToInt()
  return columns.coerceAtLeast(2)
}
