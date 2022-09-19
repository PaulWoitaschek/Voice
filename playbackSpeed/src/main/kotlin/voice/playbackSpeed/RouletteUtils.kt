package voice.playbackSpeed

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import kotlin.math.pow
import kotlin.math.roundToInt

internal object Utils {
  fun findPosition(values: List<RouletteItem>, value: Float): Int {
    var position = 0
    values.forEachIndexed { index, itemState ->
      if (itemState.value == value) {
        position = index
        return@forEachIndexed
      }
    }
    return position
  }

  fun blendColors(c1: Color, c2: Color, ration: Float): Color {
    val newColor = ColorUtils.blendARGB(c1.toArgb(), c2.toArgb(), ration)
    return Color(newColor)
  }
}

internal fun Float.roundTo(decimalPlaces: Int): Float {
  val factor = 10.0.pow(decimalPlaces.toDouble())
  return ((this * factor).roundToInt() / factor).toFloat()
}
