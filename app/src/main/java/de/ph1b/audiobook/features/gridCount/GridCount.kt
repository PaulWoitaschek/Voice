package de.ph1b.audiobook.features.gridCount

import android.content.Context
import de.ph1b.audiobook.features.bookOverview.GridMode
import de.ph1b.audiobook.features.bookOverview.GridMode.FOLLOW_DEVICE
import de.ph1b.audiobook.features.bookOverview.GridMode.GRID
import de.ph1b.audiobook.features.bookOverview.GridMode.LIST
import de.ph1b.audiobook.misc.dpToPx
import javax.inject.Inject
import kotlin.math.roundToInt

class GridCount
@Inject constructor(
  private val context: Context
) {

  fun gridColumnCount(mode: GridMode): Int {
    val useGrid = when (mode) {
      LIST -> false
      GRID -> true
      FOLLOW_DEVICE -> useGridAsDefault()
    }
    return if (useGrid) gridColumnCount() else 1
  }

  private fun useGridAsDefault(): Boolean {
    val screenWidthPx = screenWidthPx()
    val density = context.resources.displayMetrics.density
    val screenWidthDp = screenWidthPx / density
    return screenWidthDp > 450
  }

  private fun gridColumnCount(): Int {
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
