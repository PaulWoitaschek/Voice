@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.features.playbackScreen.view

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import voice.core.ui.formatTime
import voice.core.ui.icons.VoiceIcons
import voice.features.playbackScreen.BookPlayViewState
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import voice.core.strings.R as StringsR

private val HorizontalInset = 12.dp

/**
 * Seek bar for the current chapter: a wave that ripples while audio plays and flattens when paused
 * or grabbed. Dragging pops up a bubble with the target time. Tap the right label to toggle
 * between remaining and total time. After a big jump, a pill between the labels offers the way back.
 */
@Composable
internal fun SeekSection(
  playedTime: Duration,
  duration: Duration,
  playing: Boolean,
  clock: () -> Float,
  onSeek: (Duration) -> Unit,
  jumpBack: BookPlayViewState.JumpBackViewState?,
  onJumpBack: (id: Long) -> Unit,
  onJumpBackExpire: (id: Long) -> Unit,
  modifier: Modifier = Modifier,
  unavailable: Boolean = false,
) {
  var dragFraction by remember { mutableStateOf<Float?>(null) }
  var pendingFraction by remember { mutableStateOf<Float?>(null) }
  val progress = (playedTime / duration).toFloat().coerceIn(0F, 1F)
  LaunchedEffect(pendingFraction) {
    if (pendingFraction != null) {
      delay(1500.milliseconds)
      pendingFraction = null
    }
  }
  LaunchedEffect(progress) {
    val pending = pendingFraction
    if (pending != null && abs(pending - progress) < 0.01F) {
      pendingFraction = null
    }
  }
  val shownFraction = dragFraction ?: pendingFraction ?: progress
  val shownTime = duration * shownFraction.toDouble()
  val dragging = dragFraction != null
  var showRemaining by rememberSaveable { mutableStateOf(true) }

  Column(modifier = modifier) {
    WavySeekBar(
      fraction = shownFraction,
      wavy = playing && !dragging,
      dragging = dragging,
      clock = clock,
      bubbleText = formatTime(shownTime.inWholeMilliseconds, duration.inWholeMilliseconds),
      onDrag = { dragFraction = it },
      onSeek = { fraction ->
        dragFraction = null
        pendingFraction = fraction
        onSeek(duration * fraction.toDouble())
      },
      modifier = Modifier.fillMaxWidth(),
    )
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      val elapsedColor by animateColorAsState(
        targetValue = if (dragging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "elapsedColor",
      )
      RollingText(
        text = formatTime(shownTime.inWholeMilliseconds, duration.inWholeMilliseconds),
        style = MaterialTheme.typography.labelLargeEmphasized,
        color = elapsedColor,
      )
      // the pill is a bit taller than the labels, it may overlap the space around the row
      Box(
        modifier = Modifier
          .weight(1F)
          .layout { measurable, constraints ->
            val placeable = measurable.measure(constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity))
            layout(constraints.maxWidth, 0) {
              placeable.place((constraints.maxWidth - placeable.width) / 2, -placeable.height / 2)
            }
          },
      ) {
        // a jump that expired while the player was closed shows no pill, so it doesn't hide this one
        val jumpBackVisibleFor = rememberJumpBackVisibleFor()
        if (unavailable && (jumpBack == null || jumpBack.elapsed >= jumpBackVisibleFor)) {
          OfflinePill(Modifier.padding(horizontal = 4.dp))
        } else {
          JumpBackPill(
            jumpBack = jumpBack,
            onClick = onJumpBack,
            onExpire = onJumpBackExpire,
            modifier = Modifier.padding(horizontal = 4.dp),
          )
        }
      }
      RollingText(
        modifier = Modifier
          .clip(RoundedCornerShape(8.dp))
          .clickable { showRemaining = !showRemaining }
          .padding(horizontal = 4.dp, vertical = 2.dp),
        text = if (showRemaining) {
          "−" + formatTime((duration - shownTime).inWholeMilliseconds, duration.inWholeMilliseconds)
        } else {
          formatTime(duration.inWholeMilliseconds, duration.inWholeMilliseconds)
        },
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun WavySeekBar(
  fraction: Float,
  wavy: Boolean,
  dragging: Boolean,
  clock: () -> Float,
  bubbleText: String,
  onDrag: (Float) -> Unit,
  onSeek: (Float) -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = MaterialTheme.colorScheme
  val haptics = LocalHapticFeedback.current
  val amplitude by animateFloatAsState(
    targetValue = if (wavy) 1F else 0F,
    animationSpec = spring(stiffness = Spring.StiffnessVeryLow),
    label = "waveAmplitude",
  )
  val thumbWidth by animateDpAsState(
    targetValue = if (dragging) 3.dp else 5.dp,
    label = "thumbWidth",
  )
  val thumbHeight by animateDpAsState(
    targetValue = if (dragging) 40.dp else 28.dp,
    animationSpec = spring(dampingRatio = 0.45F, stiffness = Spring.StiffnessMedium),
    label = "thumbHeight",
  )
  val bubbleScale by animateFloatAsState(
    targetValue = if (dragging) 1F else 0F,
    animationSpec = spring(dampingRatio = 0.5F, stiffness = Spring.StiffnessMedium),
    label = "bubbleScale",
  )
  val currentFraction by rememberUpdatedState(fraction)
  val currentOnDrag by rememberUpdatedState(onDrag)
  val currentOnSeek by rememberUpdatedState(onSeek)
  var lastDragFraction by remember { mutableFloatStateOf(0F) }

  Layout(
    modifier = modifier,
    content = {
      Spacer(
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
          .semantics {
            progressBarRangeInfo = ProgressBarRangeInfo(current = fraction, range = 0F..1F)
            setProgress { target ->
              currentOnSeek(target.coerceIn(0F, 1F))
              true
            }
          }
          .pointerInput(Unit) {
            detectTapGestures { offset ->
              val inset = HorizontalInset.toPx()
              currentOnSeek(((offset.x - inset) / (size.width - 2 * inset)).coerceIn(0F, 1F))
            }
          }
          .pointerInput(Unit) {
            val inset = HorizontalInset.toPx()
            fun fractionAt(x: Float) = ((x - inset) / (size.width - 2 * inset)).coerceIn(0F, 1F)
            detectHorizontalDragGestures(
              onDragStart = { offset ->
                lastDragFraction = fractionAt(offset.x)
                currentOnDrag(lastDragFraction)
                haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
              },
              onDragEnd = {
                currentOnSeek(lastDragFraction)
                haptics.performHapticFeedback(HapticFeedbackType.GestureEnd)
              },
              onDragCancel = { currentOnSeek(lastDragFraction) },
              onHorizontalDrag = { change, _ ->
                change.consume()
                lastDragFraction = fractionAt(change.position.x)
                currentOnDrag(lastDragFraction)
              },
            )
          }
          .drawWithCache {
            val wavePath = Path()
            onDrawBehind {
              drawWavyTrack(
                path = wavePath,
                fraction = currentFraction,
                amplitudeFactor = amplitude,
                phase = clock() * 2F * PI.toFloat() * 0.6F,
                thumbWidth = thumbWidth.toPx(),
                thumbHeight = thumbHeight.toPx(),
                activeColor = colors.primary,
                inactiveColor = colors.secondaryContainer,
              )
            }
          },
      )
      Box(
        modifier = Modifier
          .graphicsLayer {
            scaleX = bubbleScale
            scaleY = bubbleScale
            alpha = bubbleScale.coerceIn(0F, 1F)
            transformOrigin = TransformOrigin(0.5F, 1F)
          }
          .background(colors.primary, RoundedCornerShape(percent = 50))
          .padding(horizontal = 14.dp, vertical = 6.dp),
      ) {
        Text(
          text = bubbleText,
          style = MaterialTheme.typography.labelLargeEmphasized.copy(fontFeatureSettings = "tnum"),
          color = colors.onPrimary,
        )
      }
    },
  ) { measurables, constraints ->
    val bar = measurables[0].measure(constraints)
    val bubble = measurables[1].measure(Constraints())
    layout(bar.width, bar.height) {
      bar.place(0, 0)
      val inset = HorizontalInset.toPx()
      val thumbX = inset + (bar.width - 2 * inset) * currentFraction
      val x = (thumbX - bubble.width / 2F).roundToInt()
        .coerceIn(0, (bar.width - bubble.width).coerceAtLeast(0))
      bubble.place(x, -bubble.height + 2.dp.roundToPx())
    }
  }
}

private fun DrawScope.drawWavyTrack(
  path: Path,
  fraction: Float,
  amplitudeFactor: Float,
  phase: Float,
  thumbWidth: Float,
  thumbHeight: Float,
  activeColor: Color,
  inactiveColor: Color,
) {
  val inset = HorizontalInset.toPx()
  val strokeWidth = 5.dp.toPx()
  val gap = 5.dp.toPx()
  val centerY = size.height / 2
  val end = size.width - inset
  val thumbX = inset + (end - inset) * fraction
  val amplitude = 3.5.dp.toPx() * amplitudeFactor
  val wavelength = 30.dp.toPx()
  val step = 1.5.dp.toPx()

  val activeEnd = thumbX - thumbWidth / 2 - gap
  if (activeEnd > inset) {
    path.reset()
    var x = inset
    path.moveTo(x, centerY + amplitude * sin(-phase))
    while (x < activeEnd) {
      x = min(x + step, activeEnd)
      val angle = (x - inset) / wavelength * 2F * PI.toFloat() - phase
      path.lineTo(x, centerY + amplitude * sin(angle))
    }
    drawPath(
      path = path,
      color = activeColor,
      style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
  }

  val inactiveStart = thumbX + thumbWidth / 2 + gap
  if (inactiveStart < end) {
    drawLine(
      color = inactiveColor,
      start = Offset(inactiveStart, centerY),
      end = Offset(end, centerY),
      strokeWidth = strokeWidth,
      cap = StrokeCap.Round,
    )
    drawCircle(color = activeColor, radius = 1.5.dp.toPx(), center = Offset(end, centerY))
  }

  drawRoundRect(
    color = activeColor,
    topLeft = Offset(thumbX - thumbWidth / 2, centerY - thumbHeight / 2),
    size = Size(thumbWidth, thumbHeight),
    cornerRadius = CornerRadius(thumbWidth / 2),
  )
}

/**
 * Takes the place of the jump back pill while the book can't stream.
 */
@Composable
private fun OfflinePill(modifier: Modifier = Modifier) {
  Row(
    modifier = modifier
      .clip(CircleShape)
      .background(MaterialTheme.colorScheme.surfaceContainerHighest)
      .padding(horizontal = 12.dp, vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(
      imageVector = VoiceIcons.CloudOff,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.size(16.dp),
    )
    Spacer(Modifier.width(6.dp))
    Text(
      text = stringResource(StringsR.string.library_book_offline),
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      maxLines = 1,
    )
  }
}
