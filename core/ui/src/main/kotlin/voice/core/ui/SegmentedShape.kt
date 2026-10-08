package voice.core.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

fun segmentedShape(
  index: Int,
  count: Int,
): RoundedCornerShape {
  val large = 24.dp
  val small = 6.dp
  return when {
    count == 1 -> RoundedCornerShape(large)
    index == 0 -> RoundedCornerShape(topStart = large, topEnd = large, bottomStart = small, bottomEnd = small)
    index == count - 1 -> RoundedCornerShape(topStart = small, topEnd = small, bottomStart = large, bottomEnd = large)
    else -> RoundedCornerShape(small)
  }
}
