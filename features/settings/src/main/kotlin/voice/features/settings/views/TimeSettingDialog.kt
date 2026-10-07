package voice.features.settings.views

import androidx.annotation.PluralsRes
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import kotlin.math.roundToInt
import voice.core.strings.R as StringsR

@Composable
fun TimeSettingDialog(
  title: String,
  currentSeconds: Int,
  @PluralsRes textPluralRes: Int,
  minSeconds: Int,
  maxSeconds: Int,
  onSecondsConfirm: (Int) -> Unit,
  onDismiss: () -> Unit,
) {
  val sliderState = rememberSliderState(
    value = currentSeconds.toFloat(),
    trackRange = minSeconds.toFloat()..maxSeconds.toFloat(),
  )
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(text = title)
    },
    text = {
      Column {
        Text(
          LocalResources.current.getQuantityString(
            textPluralRes,
            sliderState.value.roundToInt(),
            sliderState.value.roundToInt(),
          ),
        )
        Slider(
          state = sliderState,
          onValueChange = {
            sliderState.value = it
          },
        )
      }
    },
    confirmButton = {
      TextButton(
        onClick = {
          onSecondsConfirm(sliderState.value.roundToInt())
          onDismiss()
        },
      ) {
        Text(stringResource(StringsR.string.common_dialog_confirm))
      }
    },
    dismissButton = {
      TextButton(
        onClick = {
          onDismiss()
        },
      ) {
        Text(stringResource(StringsR.string.common_dialog_cancel))
      }
    },
  )
}
