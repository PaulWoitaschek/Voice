package voice.settings.views

import androidx.annotation.PluralsRes
import androidx.compose.foundation.layout.Column
import androidx.compose.material.AlertDialog
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import voice.settings.R
import kotlin.math.roundToInt

@Composable
fun TimeSettingDialog(
  showDialog: MutableState<Boolean>,
  title: String,
  currentSeconds: Int,
  @PluralsRes textPluralRes: Int,
  minSeconds: Int,
  maxSeconds: Int,
  onSecondsConfirmed: (Int) -> Unit
) {
  if (!showDialog.value) {
    return
  }
  val sliderValue = remember { mutableStateOf(currentSeconds.toFloat()) }
  AlertDialog(
    onDismissRequest = {
      showDialog.value = false
    },
    title = {
      ProvideTextStyle(MaterialTheme.typography.h6) {
        Text(text = title)
      }
    },
    text = {
      Column {
        ProvideTextStyle(MaterialTheme.typography.body1) {
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
      }
    },
    confirmButton = {
      TextButton(
        onClick = {
          onSecondsConfirmed(sliderValue.value.roundToInt())
          showDialog.value = false
        }
      ) {
        Text(stringResource(R.string.dialog_confirm))
      }
    },
    dismissButton = {
      TextButton(
        onClick = {
          showDialog.value = false
        }
      ) {
        Text(stringResource(R.string.dialog_cancel))
      }
    }
  )
}
