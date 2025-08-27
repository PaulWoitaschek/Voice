package voice.features.playbackScreen

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import voice.core.playback.misc.Decibel
import voice.core.strings.R as StringsR

@Composable
internal fun VolumeGainDialog(
  dialogState: BookPlayDialogViewState.VolumeGainDialog,
  viewModel: BookPlayViewModel,
) {
  AlertDialog(
    onDismissRequest = { viewModel.dismissDialog() },
    confirmButton = {},
    text = {
      Column {
        Text(stringResource(id = StringsR.string.volume_boost) + ": " + dialogState.valueFormatted)
        Slider(
          valueRange = 0F..dialogState.maxGain.value,
          value = dialogState.gain.value,
          onValueChange = {
            viewModel.onVolumeGainChanged(Decibel(it))
          },
        )
      }
    },
  )
}
