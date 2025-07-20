package voice.common.grid

import android.content.Context
import javax.inject.Inject

class GridCount
@Inject constructor(private val context: Context) {

  fun useGridAsDefault(): Boolean {
    val displayMetrics = context.resources.displayMetrics
    val screenWidthPx = displayMetrics.widthPixels.toFloat()
    val density = displayMetrics.density
    val screenWidthDp = screenWidthPx / density
    return screenWidthDp > 450
  }
}
