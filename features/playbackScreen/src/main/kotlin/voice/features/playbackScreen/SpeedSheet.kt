@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.features.playbackScreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue.Expanded
import androidx.compose.material3.SheetValue.Hidden
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import voice.core.ui.icons.VoiceIcons
import voice.features.playbackScreen.view.RollingText
import java.text.DecimalFormat
import kotlin.math.abs
import kotlin.math.roundToInt
import voice.core.strings.R as StringsR

private const val MIN_SPEED = 0.5F
private const val MAX_SPEED = 3.5F
private const val SPEED_STEP = 0.05F
private val SPEED_PRESETS = listOf(0.75F, 1F, 1.25F, 1.5F, 2F)

@Composable
internal fun SpeedSheet(
  dialogState: BookPlayDialogViewState.SpeedDialog,
  onSpeedChange: (Float) -> Unit,
  onDismiss: () -> Unit,
) {
  val speedFormat = remember { DecimalFormat("0.0#") }
  val speed = dialogState.speed
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = rememberBottomSheetState(
      initialValue = Hidden,
      enabledValues = setOf(Hidden, Expanded),
    ),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 24.dp)
        .padding(bottom = 32.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
        text = stringResource(StringsR.string.playback_speed_title),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      RollingText(
        text = speedFormat.format(speed) + "×",
        style = MaterialTheme.typography.displayLargeEmphasized,
        color = MaterialTheme.colorScheme.primary,
      )
      Spacer(Modifier.height(16.dp))
      Row(verticalAlignment = Alignment.CenterVertically) {
        FilledTonalIconButton(
          onClick = { onSpeedChange((speed - SPEED_STEP).snapToStep()) },
          shapes = IconButtonDefaults.shapes(),
          enabled = speed > MIN_SPEED,
        ) {
          Icon(VoiceIcons.Remove, contentDescription = stringResource(StringsR.string.playback_speed_decrease))
        }
        val sliderState = rememberSliderState(value = speed, trackRange = MIN_SPEED..MAX_SPEED)
        SideEffect { sliderState.value = speed }
        Slider(
          modifier = Modifier
            .weight(1F)
            .padding(horizontal = 12.dp),
          state = sliderState,
          onValueChange = {
            val snapped = it.snapToStep()
            sliderState.value = snapped
            onSpeedChange(snapped)
          },
        )
        FilledTonalIconButton(
          onClick = { onSpeedChange((speed + SPEED_STEP).snapToStep()) },
          shapes = IconButtonDefaults.shapes(),
          enabled = speed < MAX_SPEED,
        ) {
          Icon(VoiceIcons.Add, contentDescription = stringResource(StringsR.string.playback_speed_increase))
        }
      }
      Spacer(Modifier.height(16.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
      ) {
        SPEED_PRESETS.forEachIndexed { index, preset ->
          ToggleButton(
            checked = abs(speed - preset) < 0.01F,
            onCheckedChange = { onSpeedChange(preset) },
            modifier = Modifier
              .weight(1F)
              .semantics { role = Role.RadioButton },
            shapes = when (index) {
              0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
              SPEED_PRESETS.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
              else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
            },
          ) {
            Text(speedFormat.format(preset), maxLines = 1)
          }
        }
      }
    }
  }
}

private fun Float.snapToStep(): Float {
  return ((this / SPEED_STEP).roundToInt() * SPEED_STEP).coerceIn(MIN_SPEED, MAX_SPEED)
}
