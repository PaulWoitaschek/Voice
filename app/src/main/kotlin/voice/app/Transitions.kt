package voice.app

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.PathEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.get
import androidx.navigation3.scene.Scene
import voice.app.navigation.DestinationMetadataKey
import voice.navigation.Destination

/**
 * Transitions code coming from compose-pokedexer. Credits to c5inco; they look awesome!
 * https://github.com/c5inco/compose-pokedexer/blob/main/app%2Fsrc%2Fmain%2Fkotlin%2Fdes%2Fc5inco%2Fpokedexer%2Fui%2Fcommon%2FTransitions.kt#L103
 * This was originally licensed as MIT
 */

private val pathForAnimation =
  Path().apply {
    moveTo(0f, 0f)
    cubicTo(0.05f, 0f, 0.133333f, 0.06f, 0.166666f, 0.4f)
    cubicTo(0.208333f, 0.82f, 0.25f, 1f, 1f, 1f)
  }

private const val DurationMedium1 = 250
private const val DurationMedium2 = 300
private const val DurationLong1 = 450
private const val DurationLong2 = 500
private val EmphasizedEasing = PathEasing(pathForAnimation)
private val EmphasizedAccelerateEasing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

val SharedXAxisEnterTransition: (Density) -> EnterTransition = {
  fadeIn(
    animationSpec = tween(durationMillis = DurationLong1, easing = EmphasizedEasing),
  ) +
    slideInHorizontally(
      animationSpec = tween(durationMillis = DurationLong2, easing = EmphasizedEasing),
    ) {
      it / 2
    }
}

val SharedXAxisExitTransition: (Density) -> ExitTransition = { density ->
  fadeOut(
    animationSpec = tween(durationMillis = DurationMedium1, easing = EmphasizedAccelerateEasing),
  ) +
    slideOutHorizontally(
      animationSpec = tween(durationMillis = DurationMedium2, easing = EmphasizedAccelerateEasing),
    ) {
      with(density) { -30.dp.roundToPx() }
    }
}

val SharedZAxisEnterTransition =
  fadeIn(animationSpec = tween(durationMillis = DurationLong1, easing = EmphasizedEasing)) +
    scaleIn(
      initialScale = 0.8f,
      transformOrigin = TransformOrigin(0.5f, 1f),
      animationSpec = tween(durationMillis = DurationLong2, easing = EmphasizedEasing),
    )

val SharedZAxisExitTransition =
  fadeOut(animationSpec = tween(durationMillis = DurationMedium1, easing = EmphasizedAccelerateEasing)) +
    scaleOut(
      targetScale = 0.8f,
      transformOrigin = TransformOrigin(0.5f, 1f),
      animationSpec = tween(durationMillis = DurationMedium2, easing = EmphasizedAccelerateEasing),
    )

internal fun Scene<Destination.Compose>.destination(): Destination.Compose? {
  return entries.lastOrNull()?.metadata?.get(DestinationMetadataKey)
}

internal fun isBookOverviewPlaybackTransition(
  initial: Destination.Compose?,
  target: Destination.Compose?,
): Boolean {
  return (initial == Destination.BookOverview && target is Destination.Playback) ||
    (initial is Destination.Playback && target == Destination.BookOverview)
}
