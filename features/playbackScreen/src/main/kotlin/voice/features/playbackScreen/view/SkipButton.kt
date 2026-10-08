@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.features.playbackScreen.view

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import voice.core.strings.R
import voice.core.ui.icons.VoiceIcons

/** Pressing stretches the button, nudging its neighbours aside. */
@Composable
internal fun SkipButton(
  forward: Boolean,
  seconds: Int,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  height: Dp = 76.dp,
) {
  val scope = rememberCoroutineScope()
  val haptics = LocalHapticFeedback.current
  val interactionSource = remember { MutableInteractionSource() }
  val pressed by interactionSource.collectIsPressedAsState()
  val width by animateDpAsState(
    targetValue = if (pressed) height * 1.3F else height,
    animationSpec = spring(dampingRatio = 0.5F, stiffness = Spring.StiffnessMedium),
    label = "skipWidth",
  )
  val corner by animateDpAsState(
    targetValue = if (pressed) height / 4 else height / 2,
    animationSpec = spring(dampingRatio = 0.5F, stiffness = Spring.StiffnessMedium),
    label = "skipCorner",
  )
  val nudge = remember { Animatable(0F) }
  var bursts by remember { mutableStateOf(emptyList<Long>()) }
  var nextBurstId by remember { mutableLongStateOf(0L) }
  Box(modifier = modifier, contentAlignment = Alignment.Center) {
    Surface(
      onClick = {
        haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
        onClick()
        bursts = bursts + nextBurstId++
        scope.launch {
          nudge.snapTo(0F)
          nudge.animateTo(1F, tween(durationMillis = 90, easing = FastOutSlowInEasing))
          nudge.animateTo(0F, spring(dampingRatio = 0.35F, stiffness = Spring.StiffnessMediumLow))
        }
      },
      modifier = Modifier.size(width = width, height = height),
      shape = RoundedCornerShape(corner),
      color = MaterialTheme.colorScheme.secondaryContainer,
      contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
      interactionSource = interactionSource,
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          modifier = Modifier
            .size(height * 0.5F)
            .graphicsLayer {
              rotationZ = nudge.value * if (forward) 40F else -40F
              // the same circular arrows media3 uses in the notification, forward is the mirrored replay symbol
              scaleX = if (forward) -1F else 1F
            },
          imageVector = VoiceIcons.Replay,
          contentDescription = stringResource(
            id = if (forward) R.string.playback_action_fast_forward else R.string.playback_action_rewind,
          ),
        )
      }
    }
    bursts.forEach { id ->
      key(id) {
        SkipBurst(
          text = if (forward) "+$seconds" else "−$seconds",
          onFinish = { bursts = bursts - id },
          modifier = Modifier.align(Alignment.TopCenter),
        )
      }
    }
  }
}

@Composable
private fun SkipBurst(
  text: String,
  onFinish: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val progress = remember { Animatable(0F) }
  val currentOnFinish by rememberUpdatedState(onFinish)
  LaunchedEffect(progress) {
    progress.animateTo(1F, tween(durationMillis = 750, easing = FastOutSlowInEasing))
    currentOnFinish()
  }
  Text(
    modifier = modifier
      .clearAndSetSemantics {}
      .graphicsLayer {
        val p = progress.value
        translationY = -p * 56.dp.toPx()
        alpha = 1F - p
        scaleX = 0.8F + 0.5F * p
        scaleY = 0.8F + 0.5F * p
      },
    text = text,
    style = MaterialTheme.typography.titleMediumEmphasized,
    color = MaterialTheme.colorScheme.primary,
  )
}
