package voice.features.playbackScreen.view

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import voice.core.ui.formatTime
import kotlin.time.Duration

@Composable
internal fun SliderRow(
  duration: Duration,
  playedTime: Duration,
  onSeek: (Duration) -> Unit,
  onSeekDeltaChange: (Duration?) -> Unit,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    val sliderState = remember { SliderState() }
    val interactionSource = remember { MutableInteractionSource() }
    val dragging by interactionSource.collectIsDraggedAsState()
    if (!dragging) {
      sliderState.value = (playedTime / duration).toFloat().coerceIn(0F, 1F)
    }
    val sliderTime = duration * sliderState.value.toDouble()
    // captured when the drag starts so ongoing playback doesn't shift the delta
    val dragStartTime = remember(dragging) { playedTime }
    val seekDelta = if (dragging) sliderTime - dragStartTime else null
    val currentOnSeekDeltaChange by rememberUpdatedState(onSeekDeltaChange)
    LaunchedEffect(seekDelta) {
      currentOnSeekDeltaChange(seekDelta)
    }
    Text(
      text = formatTime(
        timeMs = if (dragging) {
          sliderTime.inWholeMilliseconds
        } else {
          playedTime.inWholeMilliseconds
        },
        durationMs = duration.inWholeMilliseconds,
      ),
    )
    Slider(
      modifier = Modifier
        .weight(1F)
        .padding(horizontal = 8.dp),
      state = sliderState,
      interactionSource = interactionSource,
      onValueChange = {
        sliderState.value = it
      },
      onValueChangeFinished = {
        onSeek(duration * sliderState.value.toDouble())
      },
    )
    Text(
      text = formatTime(
        timeMs = duration.inWholeMilliseconds,
        durationMs = duration.inWholeMilliseconds,
      ),
    )
  }
}
