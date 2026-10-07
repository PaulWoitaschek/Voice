@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.features.settings.views

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import voice.core.ui.icons.VoiceIcons
import voice.core.ui.rememberAnimationClock
import voice.features.settings.AUTO_REWIND_RANGE
import voice.features.settings.SEEK_TIME_RANGE
import java.text.NumberFormat
import kotlin.math.PI
import kotlin.math.sin
import voice.core.strings.R as StringsR

private val SEEK_TIME_PRESETS = listOf(10, 15, 20, 30, 60)
private val AUTO_REWIND_PRESETS = listOf(0, 2, 5, 10, 20)

/**
 * How far skipping jumps and how far resuming rewinds. Both are set right here, with a few presets
 * and fine tuning, and each comes with a little illustration of what it does.
 */
@Composable
internal fun ListeningSection(
  seekTimeInSeconds: Int,
  autoRewindInSeconds: Int,
  onSeekTimeChange: (Int) -> Unit,
  onSeekTimeStep: (Int) -> Unit,
  onAutoRewindChange: (Int) -> Unit,
  onAutoRewindStep: (Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  SettingsIsland(
    modifier = modifier,
    title = stringResource(StringsR.string.settings_listening_title),
    containerColor = MaterialTheme.colorScheme.secondaryContainer,
  ) {
    SecondsSetting(
      title = stringResource(StringsR.string.settings_playback_seek_time_title),
      summary = stringResource(StringsR.string.settings_playback_seek_time_summary),
      seconds = seekTimeInSeconds,
      range = SEEK_TIME_RANGE,
      presets = SEEK_TIME_PRESETS,
      onSecondsChange = onSeekTimeChange,
      onStep = onSeekTimeStep,
      illustration = { SkipIllustration(seekTimeInSeconds) },
    )
    Spacer(Modifier.height(28.dp))
    SecondsSetting(
      title = stringResource(StringsR.string.settings_playback_auto_rewind_title),
      summary = stringResource(StringsR.string.settings_playback_auto_rewind_summary),
      seconds = autoRewindInSeconds,
      range = AUTO_REWIND_RANGE,
      presets = AUTO_REWIND_PRESETS,
      onSecondsChange = onAutoRewindChange,
      onStep = onAutoRewindStep,
      illustration = { RewindIllustration(autoRewindInSeconds, maxSeconds = AUTO_REWIND_RANGE.last) },
    )
  }
}

@Composable
private fun SecondsSetting(
  title: String,
  summary: String,
  seconds: Int,
  range: IntRange,
  presets: List<Int>,
  onSecondsChange: (Int) -> Unit,
  onStep: (Int) -> Unit,
  illustration: @Composable () -> Unit,
) {
  Column {
    Text(
      modifier = Modifier
        .padding(horizontal = IslandContentPadding)
        .semantics { heading() },
      text = title,
      style = MaterialTheme.typography.titleMedium,
    )
    Spacer(Modifier.height(2.dp))
    Text(
      modifier = Modifier.padding(horizontal = IslandContentPadding),
      text = summary,
      style = MaterialTheme.typography.bodyMedium,
      color = LocalContentColor.current.copy(alpha = 0.8F),
    )
    Spacer(Modifier.height(12.dp))
    Row(
      modifier = Modifier.padding(horizontal = 16.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Stepper(
        increase = false,
        settingTitle = title,
        enabled = seconds > range.first,
        onClick = { onStep(-1) },
      )
      Column(
        modifier = Modifier
          .weight(1F)
          .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clearAndSetSemantics {},
          contentAlignment = Alignment.Center,
        ) {
          illustration()
        }
        Spacer(Modifier.height(6.dp))
        SecondsValue(seconds)
      }
      Stepper(
        increase = true,
        settingTitle = title,
        enabled = seconds < range.last,
        onClick = { onStep(1) },
      )
    }
    Spacer(Modifier.height(12.dp))
    SecondsPresets(
      seconds = seconds,
      presets = presets,
      onSecondsChange = onSecondsChange,
    )
  }
}

@Composable
private fun Stepper(
  increase: Boolean,
  settingTitle: String,
  enabled: Boolean,
  onClick: () -> Unit,
) {
  val action = stringResource(
    if (increase) StringsR.string.sleep_timer_dialog_action_increment else StringsR.string.sleep_timer_dialog_action_decrement,
  )
  FilledIconButton(
    onClick = onClick,
    shapes = IconButtonDefaults.shapes(),
    enabled = enabled,
    colors = IconButtonDefaults.filledIconButtonColors(
      containerColor = MaterialTheme.colorScheme.secondary,
      contentColor = MaterialTheme.colorScheme.onSecondary,
    ),
  ) {
    Icon(
      imageVector = if (increase) VoiceIcons.Add else VoiceIcons.Remove,
      // both settings have steppers, so say which one this is
      contentDescription = "$action, $settingTitle",
    )
  }
}

@Composable
private fun secondsText(seconds: Int): String {
  return if (seconds == 0) {
    stringResource(StringsR.string.settings_playback_auto_rewind_off)
  } else {
    pluralStringResource(StringsR.plurals.duration_seconds, seconds, seconds)
  }
}

/** The current value, rolling up when it grows and down when it shrinks. */
@Composable
private fun SecondsValue(seconds: Int) {
  val text = secondsText(seconds)
  AnimatedContent(
    // while rolling, both the old and the new value are shown, but only the new one is announced
    modifier = Modifier.clearAndSetSemantics {
      contentDescription = text
      liveRegion = LiveRegionMode.Polite
    },
    targetState = seconds,
    transitionSpec = {
      val up = targetState > initialState
      (slideInVertically { if (up) it else -it } + fadeIn()) togetherWith
        (slideOutVertically { if (up) -it else it } + fadeOut()) using
        SizeTransform(clip = false)
    },
    label = "secondsValue",
  ) { value ->
    Text(
      text = secondsText(value),
      style = MaterialTheme.typography.titleLargeEmphasized,
      maxLines = 1,
      autoSize = TextAutoSize.StepBased(
        minFontSize = 12.sp,
        maxFontSize = MaterialTheme.typography.titleLargeEmphasized.fontSize,
      ),
    )
  }
}

@Composable
private fun SecondsPresets(
  seconds: Int,
  presets: List<Int>,
  onSecondsChange: (Int) -> Unit,
) {
  val numberFormat = remember { NumberFormat.getIntegerInstance() }
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = IslandContentPadding),
    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
  ) {
    presets.forEachIndexed { index, preset ->
      val description = secondsText(preset)
      val labelStyle = MaterialTheme.typography.labelLarge
      // "Off" is a long word in some languages. When it doesn't fit, the number says the same.
      var wordFits by remember(description, LocalDensity.current.fontScale) { mutableStateOf(true) }
      ToggleButton(
        checked = seconds == preset,
        onCheckedChange = { onSecondsChange(preset) },
        modifier = Modifier
          .weight(1F)
          .semantics {
            role = Role.RadioButton
            contentDescription = description
          },
        shapes = when (index) {
          0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
          presets.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
          else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
        },
        colors = ToggleButtonDefaults.colors(
          containerColor = MaterialTheme.colorScheme.surfaceBright,
          contentColor = MaterialTheme.colorScheme.onSurface,
          checkedContainerColor = MaterialTheme.colorScheme.secondary,
          checkedContentColor = MaterialTheme.colorScheme.onSecondary,
        ),
        contentPadding = PaddingValues(horizontal = 4.dp),
      ) {
        Text(
          text = if (preset == 0 && wordFits) description else numberFormat.format(preset),
          style = labelStyle,
          maxLines = 1,
          softWrap = false,
          autoSize = TextAutoSize.StepBased(minFontSize = 9.sp, maxFontSize = labelStyle.fontSize),
          onTextLayout = { layout ->
            if (preset == 0 && layout.hasVisualOverflow) wordFits = false
          },
        )
      }
    }
  }
}

private class SkipBurst(
  val id: Long,
  val seconds: Int,
)

/**
 * The player's skip buttons in small. Whenever the skip amount changes, they twitch and send the new
 * amount floating up, just like skipping does in the player.
 */
@Composable
private fun SkipIllustration(seconds: Int) {
  val nudge = remember { Animatable(0F) }
  var bursts by remember { mutableStateOf(emptyList<SkipBurst>()) }
  var nextBurstId by remember { mutableLongStateOf(0L) }
  var shownSeconds by remember { mutableIntStateOf(seconds) }
  LaunchedEffect(seconds) {
    if (seconds == shownSeconds) return@LaunchedEffect
    shownSeconds = seconds
    bursts = bursts + SkipBurst(nextBurstId++, seconds)
    nudge.snapTo(0F)
    nudge.animateTo(1F, tween(durationMillis = 90, easing = FastOutSlowInEasing))
    nudge.animateTo(0F, spring(dampingRatio = 0.35F, stiffness = Spring.StiffnessMediumLow))
  }
  Row(
    horizontalArrangement = Arrangement.spacedBy(6.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    MiniSkipButton(forward = false, nudge = { nudge.value }, bursts = bursts, onBurstFinish = { bursts = bursts - it })
    Box(
      modifier = Modifier
        .size(36.dp)
        .background(MaterialTheme.colorScheme.primary, MaterialShapes.Cookie9Sided.toShape()),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        modifier = Modifier.size(20.dp),
        imageVector = VoiceIcons.PlayArrow,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onPrimary,
      )
    }
    MiniSkipButton(forward = true, nudge = { nudge.value }, bursts = bursts, onBurstFinish = { bursts = bursts - it })
  }
}

@Composable
private fun MiniSkipButton(
  forward: Boolean,
  nudge: () -> Float,
  bursts: List<SkipBurst>,
  onBurstFinish: (SkipBurst) -> Unit,
) {
  Box(contentAlignment = Alignment.Center) {
    Box(
      modifier = Modifier
        .size(width = 48.dp, height = 32.dp)
        .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(16.dp)),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        modifier = Modifier
          .size(20.dp)
          .graphicsLayer {
            rotationZ = nudge() * if (forward) 40F else -40F
            scaleX = if (forward) -1F else 1F
          },
        imageVector = VoiceIcons.Replay,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSecondary,
      )
    }
    bursts.forEach { burst ->
      key(burst.id) {
        FloatingAmount(
          text = if (forward) "+${burst.seconds}" else "−${burst.seconds}",
          onFinish = { onBurstFinish(burst) },
        )
      }
    }
  }
}

@Composable
private fun FloatingAmount(
  text: String,
  onFinish: () -> Unit,
) {
  val progress = remember { Animatable(0F) }
  val currentOnFinish by rememberUpdatedState(onFinish)
  LaunchedEffect(progress) {
    progress.animateTo(1F, tween(durationMillis = 750, easing = FastOutSlowInEasing))
    currentOnFinish()
  }
  Text(
    modifier = Modifier.graphicsLayer {
      val p = progress.value
      translationY = -p * 40.dp.toPx()
      alpha = 1F - p
      scaleX = 0.8F + 0.5F * p
      scaleY = 0.8F + 0.5F * p
    },
    text = text,
    style = MaterialTheme.typography.titleSmallEmphasized,
    color = MaterialTheme.colorScheme.primary,
  )
}

/**
 * A wavy timeline that was paused at the ghost marker. The playhead springs back from there by the
 * rewind amount, along a dashed arc, so it's clear what will be heard again.
 */
@Composable
private fun RewindIllustration(
  seconds: Int,
  maxSeconds: Int,
) {
  val clock = rememberAnimationClock(running = true)
  val target = seconds.toFloat() / maxSeconds
  val rewind = remember { Animatable(target) }
  LaunchedEffect(target) {
    if (rewind.value == target) return@LaunchedEffect
    // the jump back starts from where it was paused
    rewind.snapTo(0F)
    rewind.animateTo(target, spring(dampingRatio = 0.5F, stiffness = Spring.StiffnessLow))
  }
  val colors = MaterialTheme.colorScheme
  val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
  // the wave moves with every frame, so everything that doesn't is only created when the size changes
  Spacer(
    Modifier
      .fillMaxSize()
      .drawWithCache {
        val wave = Path()
        val arc = Path()
        val lineY = size.height * 0.72F
        val strokeWidth = 4.dp.toPx()
        val pauseX = size.width * 0.82F
        val amplitude = 2.5.dp.toPx()
        val wavelength = 16.dp.toPx()
        val waveStroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        val pauseStroke = Stroke(width = 2.dp.toPx())
        val arcStroke = Stroke(
          width = 2.dp.toPx(),
          cap = StrokeCap.Round,
          pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx())),
        )
        onDrawBehind {
          val playheadX = pauseX - rewind.value * size.width * 0.6F
          scale(scaleX = if (rtl) -1F else 1F, scaleY = 1F) {
            drawLine(
              color = colors.secondary.copy(alpha = 0.25F),
              start = Offset(strokeWidth, lineY),
              end = Offset(size.width - strokeWidth, lineY),
              strokeWidth = strokeWidth,
              cap = StrokeCap.Round,
            )
            // what was heard already
            wave.reset()
            val phase = clock.value * 2 * PI.toFloat() * 0.6F
            var x = strokeWidth
            wave.moveTo(x, lineY)
            while (x < playheadX) {
              x = (x + 2F).coerceAtMost(playheadX)
              wave.lineTo(x, lineY + amplitude * sin(x / wavelength * 2 * PI.toFloat() - phase))
            }
            drawPath(wave, color = colors.secondary, style = waveStroke)
            // where it was paused
            drawCircle(
              color = colors.onSecondaryContainer.copy(alpha = 0.45F),
              radius = 5.dp.toPx(),
              center = Offset(pauseX, lineY),
              style = pauseStroke,
            )
            val distance = pauseX - playheadX
            if (distance > 6.dp.toPx()) {
              arc.reset()
              arc.moveTo(pauseX, lineY - 8.dp.toPx())
              arc.quadraticTo(
                (pauseX + playheadX) / 2,
                lineY - 8.dp.toPx() - (distance * 0.35F).coerceAtMost(size.height * 0.6F),
                playheadX + 3.dp.toPx(),
                lineY - 10.dp.toPx(),
              )
              drawPath(path = arc, color = colors.tertiary, style = arcStroke)
            }
            drawCircle(
              color = colors.primary,
              radius = 7.dp.toPx(),
              center = Offset(playheadX, lineY),
            )
          }
        }
      },
  )
}
