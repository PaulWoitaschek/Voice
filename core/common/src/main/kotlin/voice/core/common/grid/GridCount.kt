package voice.core.common.grid

import android.content.Context
import dev.zacsweers.metro.Inject

@Inject
class GridCount(private val context: Context) {

  fun useGridAsDefault(): Boolean {
    val displayMetrics = context.resources.displayMetrics
    val screenWidthPx = displayMetrics.widthPixels.toFloat()
    val density = displayMetrics.density
    val screenWidthDp = screenWidthPx / density
    return screenWidthDp > 450
  }
}
