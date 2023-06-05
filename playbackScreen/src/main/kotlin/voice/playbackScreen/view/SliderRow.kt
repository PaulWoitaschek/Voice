package voice.playbackScreen.view

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import voice.common.formatTime
import voice.playbackScreen.BookPlayViewState
import kotlin.time.Duration

@Composable
internal fun SliderRow(
  viewState: BookPlayViewState,
  onSeek: (Duration) -> Unit,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    var localValue by remember { mutableStateOf(0F) }
    val interactionSource = remember { MutableInteractionSource() }
    val dragging by interactionSource.collectIsDraggedAsState()
    Text(
      text = formatTime(
        timeMs = if (dragging) {
          (viewState.duration * localValue.toDouble()).inWholeMilliseconds
        } else {
          viewState.playedTime.inWholeMilliseconds
        },
        durationMs = viewState.duration.inWholeMilliseconds,
      ),
    )
    Slider(
      modifier = Modifier
        .weight(1F)
        .padding(horizontal = 8.dp),
      interactionSource = interactionSource,
      value = if (dragging) {
        localValue
      } else {
        (viewState.playedTime / viewState.duration).toFloat()
          .coerceIn(0F, 1F)
      },
      onValueChange = {
        localValue = it
      },
      onValueChangeFinished = {
        onSeek(viewState.duration * localValue.toDouble())
      },
    )
    Text(
      text = formatTime(
        timeMs = viewState.duration.inWholeMilliseconds,
        durationMs = viewState.duration.inWholeMilliseconds,
      ),
    )
  }
}
