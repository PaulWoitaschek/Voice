package de.ph1b.audiobook.features.gridCount

import android.content.Context
import de.ph1b.audiobook.misc.dpToPx
import javax.inject.Inject
import kotlin.math.roundToInt

fun gridColumnCount(context: Context): Int {
  val displayMetrics = context.resources.displayMetrics
  val widthPx = displayMetrics.widthPixels.toFloat()
  val desiredPx = context.dpToPx(180F)
  val columns = (widthPx / desiredPx).roundToInt()
  return columns.coerceAtLeast(2)
}

class GridCount
@Inject constructor(private val context: Context) {

  fun useGridAsDefault(): Boolean {
    val screenWidthPx = screenWidthPx()
    val density = context.getResources().getDisplayMetrics().density
    val screenWidthDp = screenWidthPx / density
    return screenWidthDp > 450
  }

  fun gridColumnCount(): Int {
    val widthPx = screenWidthPx()
    val desiredPx = context.dpToPx(180F)
    val columns = (widthPx / desiredPx).roundToInt()
    return columns.coerceAtLeast(2)
  }

  private fun screenWidthPx(): Float {
    val displayMetrics = context.resources.displayMetrics
    return displayMetrics.widthPixels.toFloat()
  }
}
