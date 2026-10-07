@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.features.playbackScreen.view

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import voice.core.strings.R
import voice.core.ui.icons.VoiceIcons
import voice.features.playbackScreen.BookPlayViewState.JumpBackViewState

/**
 * Offers the way back after a big jump. A ring around the undo icon counts down until the pill
 * goes away by itself.
 */
@Composable
internal fun JumpBackPill(
  jumpBack: JumpBackViewState?,
  onClick: () -> Unit,
  onExpire: () -> Unit,
  modifier: Modifier = Modifier,
) {
  AnimatedContent(
    targetState = jumpBack,
    contentKey = { it?.id },
    transitionSpec = {
      (scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn())
        .togetherWith(scaleOut(targetScale = 0.8F) + fadeOut())
    },
    contentAlignment = Alignment.Center,
    modifier = modifier,
    label = "jumpBack",
  ) { state ->
    if (state != null) {
      Pill(state = state, onClick = onClick, onExpire = onExpire)
    } else {
      Spacer(Modifier.size(0.dp))
    }
  }
}

@Composable
private fun Pill(
  state: JumpBackViewState,
  onClick: () -> Unit,
  onExpire: () -> Unit,
) {
  val currentOnExpire by rememberUpdatedState(onExpire)
  val countdown = remember(state.id) {
    Animatable((state.remaining / state.visibleFor).toFloat().coerceIn(0F, 1F))
  }
  LaunchedEffect(state.id) {
    countdown.animateTo(
      targetValue = 0F,
      animationSpec = tween(durationMillis = state.remaining.inWholeMilliseconds.toInt(), easing = LinearEasing),
    )
    currentOnExpire()
  }
  val colors = MaterialTheme.colorScheme
  Surface(
    onClick = onClick,
    shape = CircleShape,
    color = colors.tertiaryContainer,
    contentColor = colors.onTertiaryContainer,
    modifier = Modifier.height(32.dp),
  ) {
    Row(
      modifier = Modifier.padding(start = 6.dp, end = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(contentAlignment = Alignment.Center, modifier = Modifier.size(22.dp)) {
        val ringColor = colors.onTertiaryContainer
        val trackColor = ringColor.copy(alpha = 0.2F)
        Canvas(Modifier.size(22.dp)) {
          val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
          drawArc(color = trackColor, startAngle = 0F, sweepAngle = 360F, useCenter = false, style = stroke)
          drawArc(
            color = ringColor,
            startAngle = -90F,
            sweepAngle = 360F * countdown.value,
            useCenter = false,
            style = stroke,
          )
        }
        Icon(
          imageVector = VoiceIcons.Undo,
          contentDescription = null,
          modifier = Modifier.size(14.dp),
        )
      }
      Spacer(Modifier.width(6.dp))
      Text(
        text = if (state.chapterNumber != null) {
          stringResource(R.string.playback_jump_back_chapter, state.chapterNumber, state.time)
        } else {
          stringResource(R.string.playback_jump_back, state.time)
        },
        style = MaterialTheme.typography.labelLargeEmphasized.copy(fontFeatureSettings = "tnum"),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}
