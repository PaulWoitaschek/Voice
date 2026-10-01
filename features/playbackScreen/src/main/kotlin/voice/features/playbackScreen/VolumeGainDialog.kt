package voice.features.playbackScreen

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSliderState
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
        Text(stringResource(id = StringsR.string.playback_option_volume_boost) + ": " + dialogState.valueFormatted)
        val sliderState = rememberSliderState(
          value = dialogState.gain.value,
          trackRange = 0F..dialogState.maxGain.value,
        )
        Slider(
          state = sliderState,
          onValueChange = {
            sliderState.value = it
            viewModel.onVolumeGainChanged(Decibel(it))
          },
        )
      }
    },
  )
}
