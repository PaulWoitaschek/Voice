/*
 * This file contains code copied or adapted from:
 * https://github.com/c5inco/compose-pokedexer/blob/d516f64e62d29d14e5ea82fdd6bf0a7d274287cc/app/src/main/kotlin/des/c5inco/pokedexer/ui/common/Transitions.kt
 *
 * Original project:
 * compose-pokedexer by Chris Sinco
 * https://github.com/c5inco/compose-pokedexer
 *
 * MIT License
 *
 * Copyright (c) 2022 Chris Sinco
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * Modifications:
 * Copyright (c) 2026 Paul Woitaschek
 *
 * This file is distributed as part of Voice under the GNU GPL v3.
 */
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

private val pathForAnimation =
  Path().apply {
    moveTo(0f, 0f)
    cubicTo(0.05f, 0f, 0.133333f, 0.06f, 0.166666f, 0.4f)
    cubicTo(0.208333f, 0.82f, 0.25f, 1f, 1f, 1f)
  }

@Suppress("ktlint:standard:property-naming")
private const val DurationMedium1 = 250

@Suppress("ktlint:standard:property-naming")
private const val DurationMedium2 = 300

@Suppress("ktlint:standard:property-naming")
private const val DurationLong1 = 450

@Suppress("ktlint:standard:property-naming")
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
