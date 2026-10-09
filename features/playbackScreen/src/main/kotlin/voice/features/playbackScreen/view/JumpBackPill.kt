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
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import voice.core.strings.R
import voice.core.ui.icons.VoiceIcons
import voice.features.playbackScreen.BookPlayViewState.JumpBackViewState
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private val JUMP_BACK_VISIBLE = 10.seconds

/** How long the pill stays, longer for people who need more time to react. */
@Composable
internal fun rememberJumpBackVisibleFor(): Duration {
  val accessibilityManager = LocalAccessibilityManager.current
  return remember(accessibilityManager) {
    val millis = JUMP_BACK_VISIBLE.inWholeMilliseconds
    (
      accessibilityManager?.calculateRecommendedTimeoutMillis(
        originalTimeoutMillis = millis,
        containsIcons = true,
        containsText = true,
        containsControls = true,
      ) ?: millis
      ).milliseconds
  }
}

@Composable
internal fun JumpBackPill(
  jumpBack: JumpBackViewState?,
  onClick: (id: Long) -> Unit,
  onExpire: (id: Long) -> Unit,
  modifier: Modifier = Modifier,
) {
  val visibleFor = rememberJumpBackVisibleFor()
  AnimatedContent(
    targetState = jumpBack?.takeIf { it.elapsed < visibleFor },
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
      Pill(
        state = state,
        visibleFor = visibleFor,
        onClick = { onClick(state.id) },
        onExpire = { onExpire(state.id) },
      )
    } else {
      Spacer(Modifier.size(0.dp))
    }
  }
}

@Composable
private fun Pill(
  state: JumpBackViewState,
  visibleFor: Duration,
  onClick: () -> Unit,
  onExpire: () -> Unit,
) {
  val currentOnExpire by rememberUpdatedState(onExpire)
  val remaining = remember(state.id) { (visibleFor - state.elapsed).coerceAtLeast(Duration.ZERO) }
  val countdown = remember(state.id) {
    Animatable((remaining / visibleFor).toFloat().coerceIn(0F, 1F))
  }
  LaunchedEffect(state.id) {
    // the ring is only decoration, so it may skip ahead when animations are off
    launch {
      countdown.animateTo(
        targetValue = 0F,
        animationSpec = tween(durationMillis = remaining.inWholeMilliseconds.toInt(), easing = LinearEasing),
      )
    }
    delay(remaining)
    currentOnExpire()
  }
  val colors = MaterialTheme.colorScheme
  Surface(
    onClick = onClick,
    shape = CircleShape,
    color = colors.tertiaryContainer,
    contentColor = colors.onTertiaryContainer,
    modifier = Modifier
      .height(32.dp)
      .semantics { liveRegion = LiveRegionMode.Polite },
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
