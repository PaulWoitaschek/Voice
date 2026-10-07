package voice.core.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/** Drives a staggered entrance: sections float up one after another when the screen opens. */
@Stable
class EntranceState internal constructor(initialProgress: Float) {
  internal val progress = Animatable(initialProgress)
}

/** Plays the entrance once composed. Without [animate], everything shows right away. */
@Composable
fun rememberEntranceState(animate: Boolean = true): EntranceState {
  val state = remember { EntranceState(initialProgress = if (animate) 0F else 1F) }
  LaunchedEffect(state) {
    state.progress.animateTo(1F, tween(durationMillis = 900, easing = LinearEasing))
  }
  return state
}

/** Sections with a higher [index] enter later. */
fun Modifier.entrance(
  state: EntranceState,
  index: Int,
): Modifier = graphicsLayer {
  val local = ((state.progress.value - index * 0.08F) / 0.5F).coerceIn(0F, 1F)
  alpha = local
  translationY = (1F - EaseOutBack.transform(local)) * 40.dp.toPx()
}
