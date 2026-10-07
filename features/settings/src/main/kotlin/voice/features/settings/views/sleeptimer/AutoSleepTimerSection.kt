@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.features.settings.views.sleeptimer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.graphics.shapes.RoundedPolygon
import voice.core.data.ThemeColorScheme
import voice.core.ui.AuroraBackground
import voice.core.ui.NightTheme
import voice.core.ui.ShapedIcon
import voice.core.ui.VoiceTheme
import voice.core.ui.icons.VoiceIcons
import voice.core.ui.rememberAnimationClock
import voice.features.settings.SettingsViewState
import voice.features.settings.views.IslandShape
import voice.features.settings.views.IslandSwitchRow
import voice.features.settings.views.TimePickerDialog
import java.time.LocalTime
import voice.core.strings.R as StringsR

/**
 * The automatic sleep timer. Turning it on turns the island into a night sky with twinkling stars,
 * whatever the app's theme. The moon and the sun pick when the night starts and ends.
 */
@Composable
internal fun AutoSleepTimerSection(
  viewState: SettingsViewState.AutoSleepTimerViewState,
  themeColorScheme: ThemeColorScheme,
  onEnabledChange: (Boolean) -> Unit,
  onStartChange: (LocalTime) -> Unit,
  onEndChange: (LocalTime) -> Unit,
  modifier: Modifier = Modifier,
) {
  val night = viewState.enabled
  NightTheme(themeColorScheme = themeColorScheme, night = night) {
    // read while drawing, so fading doesn't recompose everything with every frame
    val skyAlpha = animateFloatAsState(
      targetValue = if (night) 1F else 0F,
      animationSpec = tween(durationMillis = 1200),
      label = "skyAlpha",
    )
    val showSky by remember { derivedStateOf { skyAlpha.value > 0F } }
    val clock = rememberAnimationClock(running = night)
    Surface(
      modifier = modifier.fillMaxWidth(),
      shape = IslandShape,
      color = MaterialTheme.colorScheme.surfaceContainerHigh,
      contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
      Box {
        if (showSky) {
          AuroraBackground(
            clock = { clock.value },
            showStars = night,
            modifier = Modifier
              .matchParentSize()
              .graphicsLayer { alpha = skyAlpha.value },
          )
        }
        Column(Modifier.padding(vertical = 12.dp)) {
          val timeFormatter = rememberLocalTimeFormatter()
          IslandSwitchRow(
            title = stringResource(StringsR.string.settings_auto_sleep_timer_title),
            titleStyle = MaterialTheme.typography.titleLargeEmphasized,
            checked = night,
            onCheckedChange = onEnabledChange,
            leading = {
              ShapedIcon(
                icon = VoiceIcons.Bedtime,
                shape = MaterialShapes.Cookie7Sided,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
              )
            },
            summary = {
              Text(
                stringResource(
                  StringsR.string.settings_auto_sleep_timer_summary,
                  timeFormatter.format(viewState.startTime),
                  timeFormatter.format(viewState.endTime),
                ),
              )
            },
          )
          Spacer(Modifier.height(8.dp))
          Row(
            modifier = Modifier.padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            TimeTile(
              modifier = Modifier.weight(1F),
              icon = VoiceIcons.Bedtime,
              shape = MaterialShapes.Circle,
              iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
              iconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
              time = viewState.startTime,
              label = stringResource(StringsR.string.settings_auto_sleep_timer_start_label),
              enabled = night,
              onTimeChange = onStartChange,
            )
            TimeTile(
              modifier = Modifier.weight(1F),
              icon = VoiceIcons.LightMode,
              shape = MaterialShapes.Sunny,
              iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
              iconContentColor = MaterialTheme.colorScheme.onTertiaryContainer,
              time = viewState.endTime,
              label = stringResource(StringsR.string.settings_auto_sleep_timer_end_label),
              enabled = night,
              onTimeChange = onEndChange,
            )
          }
          AnimatedVisibility(visible = night) {
            ShakeHint()
          }
          Spacer(Modifier.height(12.dp))
        }
      }
    }
  }
}

/** The [time] the night starts or ends. Tapping it opens a time picker. */
@Composable
private fun TimeTile(
  icon: ImageVector,
  shape: RoundedPolygon,
  iconContainerColor: Color,
  iconContentColor: Color,
  time: LocalTime,
  label: String,
  enabled: Boolean,
  onTimeChange: (LocalTime) -> Unit,
  modifier: Modifier = Modifier,
) {
  var showTimePicker by remember { mutableStateOf(false) }
  val alpha by animateFloatAsState(
    targetValue = if (enabled) 1F else 0.5F,
    label = "timeTileAlpha",
  )
  val timeFormatter = rememberLocalTimeFormatter()
  Column(
    modifier = modifier
      .graphicsLayer { this.alpha = alpha }
      .clip(RoundedCornerShape(24.dp))
      .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.8F))
      .clickable(
        enabled = enabled,
        onClickLabel = label,
        role = Role.Button,
        onClick = { showTimePicker = true },
      )
      .padding(16.dp),
  ) {
    ShapedIcon(
      icon = icon,
      shape = shape,
      containerColor = iconContainerColor,
      contentColor = iconContentColor,
      size = 40.dp,
    )
    Spacer(Modifier.height(12.dp))
    val style = MaterialTheme.typography.headlineMediumEmphasized
    Text(
      text = timeFormatter.format(time),
      style = style,
      maxLines = 1,
      autoSize = TextAutoSize.StepBased(minFontSize = 14.sp, maxFontSize = style.fontSize),
    )
  }
  if (showTimePicker) {
    TimePickerDialog(
      initialHour = time.hour,
      initialMinute = time.minute,
      onConfirm = { timePickerState ->
        onTimeChange(LocalTime.of(timePickerState.hour, timePickerState.minute))
        showTimePicker = false
      },
      onDismiss = {
        showTimePicker = false
      },
    )
  }
}

/** Restarting the timer by shaking the phone is easy to miss, so it's pointed out here. */
@Composable
private fun ShakeHint() {
  Row(
    modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(
      modifier = Modifier.size(20.dp),
      imageVector = VoiceIcons.Vibration,
      contentDescription = null,
    )
    Text(
      text = stringResource(StringsR.string.sleep_timer_dialog_shake_hint),
      style = MaterialTheme.typography.bodyMedium,
      color = LocalContentColor.current.copy(alpha = 0.8F),
    )
  }
}

@Composable
@Preview
private fun AutoSleepTimerSectionPreview() {
  VoiceTheme {
    AutoSleepTimerSection(
      viewState = SettingsViewState.AutoSleepTimerViewState.preview().copy(enabled = true),
      themeColorScheme = ThemeColorScheme.VoiceBlue,
      onEnabledChange = {},
      onStartChange = {},
      onEndChange = {},
    )
  }
}
