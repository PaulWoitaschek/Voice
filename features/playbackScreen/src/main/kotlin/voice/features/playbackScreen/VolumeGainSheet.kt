@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.features.playbackScreen

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue.Expanded
import androidx.compose.material3.SheetValue.Hidden
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import voice.core.playback.misc.Decibel
import voice.features.playbackScreen.view.RollingText
import kotlin.math.ceil
import voice.core.strings.R as StringsR

private const val METER_BARS = 18

@Composable
internal fun VolumeGainSheet(
  dialogState: BookPlayDialogViewState.VolumeGainDialog,
  onGainChange: (Decibel) -> Unit,
  onDismiss: () -> Unit,
) {
  val fraction = (dialogState.gain.value / dialogState.maxGain.value).coerceIn(0F, 1F)
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
        modifier = Modifier.fillMaxWidth(),
        text = stringResource(StringsR.string.playback_option_volume_boost),
        style = MaterialTheme.typography.headlineSmallEmphasized,
        textAlign = TextAlign.Center,
      )
      Spacer(Modifier.height(16.dp))
      RollingText(
        text = "+" + dialogState.valueFormatted,
        style = MaterialTheme.typography.displayMediumEmphasized,
        color = MaterialTheme.colorScheme.primary,
      )
      Spacer(Modifier.height(20.dp))
      GainMeter(fraction = fraction)
      Spacer(Modifier.height(12.dp))
      val sliderState = rememberSliderState(value = dialogState.gain.value, trackRange = 0F..dialogState.maxGain.value)
      SideEffect { sliderState.value = dialogState.gain.value }
      Slider(
        modifier = Modifier.fillMaxWidth(),
        state = sliderState,
        onValueChange = {
          sliderState.value = it
          onGainChange(Decibel(it))
        },
      )
      Spacer(Modifier.height(12.dp))
      Button(
        onClick = { onGainChange(Decibel(0F)) },
        enabled = dialogState.gain.value > 0F,
        shapes = ButtonDefaults.shapes(),
      ) {
        Text(stringResource(StringsR.string.playback_volume_boost_off))
      }
    }
  }
}

@Composable
private fun GainMeter(
  fraction: Float,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .height(56.dp)
      .clearAndSetSemantics {},
    horizontalArrangement = Arrangement.spacedBy(4.dp),
    verticalAlignment = Alignment.Bottom,
  ) {
    repeat(METER_BARS) { index ->
      val lit = fraction > 0F && index < ceil(fraction * METER_BARS).toInt()
      val color by animateColorAsState(
        targetValue = if (lit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
        label = "meterColor",
      )
      val baseHeight = 12.dp + (44.dp - 12.dp) * (index / (METER_BARS - 1F))
      val height by animateDpAsState(
        targetValue = if (lit) baseHeight else baseHeight * 0.8F,
        animationSpec = spring(dampingRatio = 0.4F, stiffness = Spring.StiffnessMedium),
        label = "meterHeight",
      )
      Box(
        modifier = Modifier
          .weight(1F)
          .height(height)
          .background(color, RoundedCornerShape(percent = 50)),
      )
    }
  }
}
