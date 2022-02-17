package voice.bookOverview

import android.content.Context
import voice.bookOverview.GridMode.FOLLOW_DEVICE
import voice.bookOverview.GridMode.GRID
import voice.bookOverview.GridMode.LIST
import voice.common.dpToPx
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
