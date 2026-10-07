@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.core.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import voice.core.strings.R as StringsR

/**
 * The hero play button. Paused it is a playful scalloped cookie that invites a tap; playing it
 * settles into a calm rounded square. Every toggle spins it a quarter turn and presses squish it.
 *
 * The icon scales with the button's measured size, so it stays proportional while a shared element
 * transition animates the button between screens.
 */
@Composable
fun PlayButton(
  playing: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  size: Dp = 104.dp,
) {
  val haptics = LocalHapticFeedback.current
  val interactionSource = remember { MutableInteractionSource() }
  val pressed by interactionSource.collectIsPressedAsState()
  val morph = remember { Morph(MaterialShapes.Cookie9Sided, MaterialShapes.Square) }
  val morphProgress by animateFloatAsState(
    targetValue = if (playing) 1F else 0F,
    animationSpec = spring(dampingRatio = 0.6F, stiffness = Spring.StiffnessMediumLow),
    label = "playMorph",
  )
  val scale by animateFloatAsState(
    targetValue = if (pressed) 0.86F else 1F,
    animationSpec = spring(dampingRatio = 0.4F, stiffness = Spring.StiffnessMedium),
    label = "playScale",
  )
  var turns by remember { mutableIntStateOf(0) }
  val rotation by animateFloatAsState(
    targetValue = turns * 90F,
    animationSpec = spring(dampingRatio = 0.55F, stiffness = Spring.StiffnessLow),
    label = "playRotation",
  )
  val containerColor = MaterialTheme.colorScheme.primary
  val contentColor = MaterialTheme.colorScheme.onPrimary
  Box(
    modifier = modifier
      .size(size)
      .graphicsLayer {
        scaleX = scale
        scaleY = scale
        rotationZ = rotation
      }
      .clip(MorphShape(morph, morphProgress.coerceIn(0F, 1F)))
      .background(containerColor)
      .clickable(
        interactionSource = interactionSource,
        indication = ripple(color = contentColor),
        role = Role.Button,
      ) {
        turns++
        haptics.performHapticFeedback(if (playing) HapticFeedbackType.ToggleOff else HapticFeedbackType.ToggleOn)
        onClick()
      },
    contentAlignment = Alignment.Center,
  ) {
    // animated vectors always mirror in right to left layouts, but a play arrow never points left
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
      Icon(
        modifier = Modifier
          .fillMaxSize(0.4F)
          .graphicsLayer { rotationZ = -rotation },
        painter = rememberPlayPausePainter(playing),
        contentDescription = stringResource(
          id = if (playing) StringsR.string.playback_action_pause else StringsR.string.playback_action_play,
        ),
        tint = contentColor,
      )
    }
  }
}

@Composable
private fun rememberPlayPausePainter(playing: Boolean): Painter {
  return rememberAnimatedVectorPainter(
    animatedImageVector = AnimatedImageVector.animatedVectorResource(id = R.drawable.avd_pause_to_play),
    atEnd = !playing,
  )
}
