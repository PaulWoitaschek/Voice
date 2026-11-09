package voice.core.ui

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun segmentedShape(
  index: Int,
  count: Int,
): CornerBasedShape {
  val outer = MaterialTheme.shapes.largeIncreased
  val inner = MaterialTheme.shapes.extraSmall
  return when {
    count == 1 -> outer
    index == 0 -> outer.copy(bottomStart = inner.bottomStart, bottomEnd = inner.bottomEnd)
    index == count - 1 -> outer.copy(topStart = inner.topStart, topEnd = inner.topEnd)
    else -> inner
  }
}
