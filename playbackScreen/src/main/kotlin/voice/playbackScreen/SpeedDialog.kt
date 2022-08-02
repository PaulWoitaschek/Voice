package voice.playbackScreen

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import java.text.DecimalFormat

@Composable
internal fun SpeedDialog(
  dialogState: BookPlayDialogViewState.SpeedDialog,
  viewModel: BookPlayViewModel,
) {
  val speedFormatter = remember { DecimalFormat("0.00 x") }

  AlertDialog(
    onDismissRequest = { viewModel.dismissDialog() },
    confirmButton = {},
    title = {
      Text(stringResource(id = R.string.playback_speed))
    },
    text = {
      Column {
        Text(stringResource(id = R.string.playback_speed) + ": " + speedFormatter.format(dialogState.speed))
        val valueRange = 0.5F..dialogState.maxSpeed
        val rangeSize = valueRange.endInclusive - valueRange.start
        val stepSize = 0.05
        val steps = (rangeSize / stepSize).toInt() - 1
        Slider(
          steps = steps,
          valueRange = valueRange,
          value = dialogState.speed,
          onValueChange = {
            viewModel.onPlaybackSpeedChanged(it)
          },
        )
      }
    },
  )
}
