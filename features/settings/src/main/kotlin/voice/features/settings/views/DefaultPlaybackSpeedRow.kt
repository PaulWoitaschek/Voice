package voice.features.settings.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import voice.core.ui.icons.VoiceIcons
import java.text.DecimalFormat
import kotlin.math.roundToInt
import voice.core.strings.R as StringsR

private const val MinSpeed = 0.5F
private const val MaxSpeed = 3.5F
private const val SpeedStep = 0.05F

@Composable
internal fun DefaultPlaybackSpeedRow(
  speed: Float,
  openDialog: () -> Unit,
) {
  val speedFormatter = remember { DecimalFormat("0.00 x") }
  ListItem(
    modifier = Modifier
      .clickable { openDialog() }
      .fillMaxWidth(),
    leadingContent = {
      Icon(
        imageVector = VoiceIcons.Speed,
        contentDescription = stringResource(StringsR.string.settings_playback_default_speed_title),
      )
    },
    supportingContent = {
      Text(text = speedFormatter.format(speed))
    },
  ) {
    Text(text = stringResource(StringsR.string.settings_playback_default_speed_title))
  }
}

@Composable
internal fun DefaultPlaybackSpeedDialog(
  currentSpeed: Float,
  onSpeedConfirm: (Float) -> Unit,
  onDismiss: () -> Unit,
) {
  val speedFormatter = remember { DecimalFormat("0.00 x") }
  val steps = ((MaxSpeed - MinSpeed) / SpeedStep).roundToInt() - 1
  val sliderState = rememberSliderState(
    value = currentSpeed.coerceIn(MinSpeed, MaxSpeed),
    steps = steps,
    trackRange = MinSpeed..MaxSpeed,
  )
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(text = stringResource(StringsR.string.settings_playback_default_speed_title))
    },
    text = {
      Column {
        Text(text = speedFormatter.format(sliderState.value))
        Slider(
          state = sliderState,
          onValueChange = { sliderState.value = it },
        )
      }
    },
    confirmButton = {
      TextButton(
        onClick = {
          onSpeedConfirm((sliderState.value / SpeedStep).roundToInt() * SpeedStep)
          onDismiss()
        },
      ) {
        Text(stringResource(StringsR.string.common_dialog_confirm))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(StringsR.string.common_dialog_cancel))
      }
    },
  )
}
