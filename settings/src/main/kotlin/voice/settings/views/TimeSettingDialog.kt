package voice.settings.views

import androidx.annotation.PluralsRes
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Slider
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import voice.settings.R
import kotlin.math.roundToInt

@Composable
fun TimeSettingDialog(
  title: String,
  currentSeconds: Int,
  @PluralsRes textPluralRes: Int,
  minSeconds: Int,
  maxSeconds: Int,
  onSecondsConfirmed: (Int) -> Unit,
  onDismiss: () -> Unit
) {
  val sliderValue = remember { mutableStateOf(currentSeconds.toFloat()) }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(text = title)
    },
    text = {
      Column {
        Text(
          LocalContext.current.resources.getQuantityString(
            textPluralRes,
            sliderValue.value.roundToInt(),
            sliderValue.value.roundToInt()
          )
        )
      }
      Slider(
        valueRange = minSeconds.toFloat()..maxSeconds.toFloat(),
        value = sliderValue.value,
        onValueChange = {
          sliderValue.value = it
        }
      )
    },
    confirmButton = {
      TextButton(
        onClick = {
          onSecondsConfirmed(sliderValue.value.roundToInt())
          onDismiss()
        }
      ) {
        Text(stringResource(R.string.dialog_confirm))
      }
    },
    dismissButton = {
      TextButton(
        onClick = {
          onDismiss()
        }
      ) {
        Text(stringResource(R.string.dialog_cancel))
      }
    }
  )
}
