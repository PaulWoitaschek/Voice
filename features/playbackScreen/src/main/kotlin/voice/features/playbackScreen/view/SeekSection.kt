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
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import voice.core.strings.R as StringsR
import voice.core.ui.ProgressWave
import voice.core.ui.R as UiR
import voice.core.ui.formatTime
import voice.features.playbackScreen.BookPlayViewState

private val HorizontalInset = 12.dp
private val AccessibilitySeekStep = 30.seconds

/**
 * Seek bar for the current chapter: a wave that ripples while audio plays and flattens when paused
 * or grabbed. Dragging pops up a bubble with the target time. Tap the right label to toggle between
 * remaining and total time. After a big jump, a pill between the labels offers the way back.
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
  bookPercent: String? = null,
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
  val shownTimeText = formatTime(shownTime.inWholeMilliseconds, duration.inWholeMilliseconds)
  val durationText = formatTime(duration.inWholeMilliseconds, duration.inWholeMilliseconds)
  var seekedTime by remember(duration, playing) { mutableStateOf<Duration?>(null) }
  // TalkBack reads out each change of the bar, so while it plays it changes once a minute.
  // After a seek it says the exact time, else a swipe in the same minute gets no answer.
  val spokenTime =
    when {
      !playing || dragging -> shownTime
      seekedTime?.inWholeMinutes == shownTime.inWholeMinutes -> seekedTime
      else -> null
    }
  val spokenMinutes = shownTime.inWholeMinutes

  Column(modifier = modifier) {
    WavySeekBar(
      fraction = shownFraction,
      wavy = playing && !dragging,
      dragging = dragging,
      clock = clock,
      bubbleText = shownTimeText,
      accessibilityFraction =
        if (spokenTime != null) {
          (spokenTime / duration).toFloat()
        } else {
          // above 0, as TalkBack only offers a swipe down above the start
          (spokenMinutes.minutes / duration).toFloat().coerceAtLeast(Float.MIN_VALUE)
        },
      stateDescription =
        if (spokenTime != null) {
          stringResource(
            StringsR.string.playback_seek_bar_position,
            formatTime(spokenTime.inWholeMilliseconds, duration.inWholeMilliseconds),
            durationText,
          )
        } else {
          pluralStringResource(
            StringsR.plurals.playback_seek_bar_position_minutes,
            spokenMinutes.toInt(),
            spokenMinutes.toInt(),
            durationText,
          )
        },
      accessibilitySteps = ((duration / AccessibilitySeekStep).roundToInt() - 1).coerceAtLeast(0),
      onDrag = { dragFraction = it },
      onSeek = { fraction ->
        dragFraction = null
        pendingFraction = fraction
        val target = duration * fraction.toDouble()
        seekedTime = target
        onSeek(target)
      },
      modifier = Modifier.fillMaxWidth(),
    )
    val elapsedColor by
      animateColorAsState(
        targetValue =
          if (dragging) MaterialTheme.colorScheme.primary
          else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "elapsedColor",
      )
    // a jump that expired while the player was closed shows no pill, so it doesn't hide this one
    val jumpBackVisibleFor = rememberJumpBackVisibleFor()
    val jumpBackShown = jumpBack != null && jumpBack.elapsed < jumpBackVisibleFor
    Layout(
      contents =
        listOf(
          {
            Text(
              modifier = Modifier.testTag("elapsed"),
              text = shownTimeText,
              style =
                MaterialTheme.typography.labelLargeEmphasized.copy(fontFeatureSettings = "tnum"),
              color = elapsedColor,
            )
          },
          {
            if (unavailable && !jumpBackShown) {
              OfflinePill(Modifier.padding(horizontal = 4.dp))
            } else {
              JumpBackPill(
                jumpBack = jumpBack,
                onClick = onJumpBack,
                onExpire = onJumpBackExpire,
                modifier = Modifier.padding(horizontal = 4.dp),
              )
            }
          },
          {
            Text(
              modifier =
                Modifier.clip(MaterialTheme.shapes.small)
                  .clickable { showRemaining = !showRemaining }
                  .padding(horizontal = 4.dp, vertical = 2.dp),
              text =
                if (showRemaining) {
                  "−" +
                    formatTime(
                      (duration - shownTime).inWholeMilliseconds,
                      duration.inWholeMilliseconds,
                    )
                } else {
                  durationText
                },
              style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          },
          {
            if (bookPercent != null) {
              val percentAlpha by
                animateFloatAsState(
                  targetValue = if (jumpBackShown || unavailable) 0F else 1F,
                  label = "bookPercentAlpha",
                )
              Text(
                text = bookPercent,
                style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.graphicsLayer { alpha = percentAlpha },
              )
            }
          },
        ),
      modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
    ) { (elapsedSlot, pillSlot, remainingSlot, percentSlot), constraints ->
      val width = constraints.maxWidth
      val loose = constraints.copy(minWidth = 0, minHeight = 0)
      val elapsed = elapsedSlot.single().measure(loose)
      val remaining = remainingSlot.single().measure(loose)
      val pill =
        pillSlot
          .firstOrNull()
          ?.measure(
            Constraints(maxWidth = (width - elapsed.width - remaining.width).coerceAtLeast(0)),
          )
      val percent = percentSlot.firstOrNull()?.measure(loose)
      val height = maxOf(elapsed.height, remaining.height)
      val gap = 8.dp.roundToPx()
      layout(width, height) {
        fun Placeable.placeAt(x: Int) = place(x, (height - this.height) / 2)
        elapsed.placeAt(0)
        remaining.placeAt(width - remaining.width)
        // the pill is a bit taller than the labels, it may overlap the space around the row
        pill?.placeAt(elapsed.width)
        // under the middle of the bar rather than between the times, unless that would run into
        // them
        if (percent != null) {
          val x = (width - percent.width) / 2
          if (x >= elapsed.width + gap && x + percent.width + gap <= width - remaining.width)
            percent.placeAt(x)
        }
      }
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
  accessibilityFraction: Float,
  stateDescription: String,
  accessibilitySteps: Int,
  onDrag: (Float) -> Unit,
  onSeek: (Float) -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = MaterialTheme.colorScheme
  val haptics = LocalHapticFeedback.current
  val amplitude by
    animateFloatAsState(
      targetValue = if (wavy) 1F else 0F,
      animationSpec = spring(stiffness = Spring.StiffnessVeryLow),
      label = "waveAmplitude",
    )
  val thumbWidth by
    animateDpAsState(
      targetValue = if (dragging) 3.dp else 5.dp,
      label = "thumbWidth",
    )
  val thumbHeight by
    animateDpAsState(
      targetValue = if (dragging) 40.dp else 28.dp,
      animationSpec = spring(dampingRatio = 0.45F, stiffness = Spring.StiffnessMedium),
      label = "thumbHeight",
    )
  val bubbleScale by
    animateFloatAsState(
      targetValue = if (dragging) 1F else 0F,
      animationSpec = spring(dampingRatio = 0.5F, stiffness = Spring.StiffnessMedium),
      label = "bubbleScale",
    )
  val currentFraction by rememberUpdatedState(fraction)
  val currentOnDrag by rememberUpdatedState(onDrag)
  val currentOnSeek by rememberUpdatedState(onSeek)
  var lastDragFraction by remember { mutableFloatStateOf(0F) }
  val name = stringResource(StringsR.string.playback_seek_bar)

  Layout(
    modifier = modifier,
    content = {
      Spacer(
        modifier =
          Modifier.fillMaxWidth()
            .height(48.dp)
            .semantics {
              contentDescription = name
              this.stateDescription = stateDescription
              progressBarRangeInfo =
                ProgressBarRangeInfo(
                  current = accessibilityFraction,
                  range = 0F..1F,
                  steps = accessibilitySteps,
                )
              // a step moves from the real position, not from the whole minute
              setProgress { target ->
                currentOnSeek((currentFraction + target - accessibilityFraction).coerceIn(0F, 1F))
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
                  phase = clock() * 2F * PI.toFloat() * ProgressWave.WavesPerSecond,
                  thumbWidth = thumbWidth.toPx(),
                  thumbHeight = thumbHeight.toPx(),
                  activeColor = colors.primary,
                  inactiveColor = colors.secondaryContainer,
                )
              }
            },
      )
      Box(
        modifier =
          Modifier.graphicsLayer {
              scaleX = bubbleScale
              scaleY = bubbleScale
              alpha = bubbleScale.coerceIn(0F, 1F)
              transformOrigin = TransformOrigin(0.5F, 1F)
            }
            .background(colors.primary, CircleShape)
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
      val x =
        (thumbX - bubble.width / 2F)
          .roundToInt()
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
  val strokeWidth = ProgressWave.StrokeWidth.toPx()
  val gap = 5.dp.toPx()
  val centerY = size.height / 2
  val end = size.width - inset
  val thumbX = inset + (end - inset) * fraction
  val amplitude = ProgressWave.Amplitude.toPx() * amplitudeFactor
  val wavelength = ProgressWave.Wavelength.toPx()
  val step = 1.5.dp.toPx()

  val activeEnd = thumbX - thumbWidth / 2 - gap
  if (activeEnd > inset) {
    path.reset()
    var x = inset
    path.moveTo(x, centerY + amplitude * sin(phase))
    while (x < activeEnd) {
      x = min(x + step, activeEnd)
      val angle = (x - inset) / wavelength * 2F * PI.toFloat() + phase
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

/** Takes the place of the jump back pill while the book can't stream. */
@Composable
private fun OfflinePill(modifier: Modifier = Modifier) {
  Row(
    modifier =
      modifier
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
        .padding(horizontal = 12.dp, vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(
      painter = painterResource(UiR.drawable.ic_cloud_off),
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
