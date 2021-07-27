package voice.playbackScreen.views

import androidx.compose.material.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import voice.playbackScreen.BookPlayListener
import voice.playbackScreen.BookPlayViewState
import kotlin.math.roundToLong
import kotlin.time.DurationUnit

@Composable
internal fun PlaybackSlider(
  viewState: BookPlayViewState,
  listener: BookPlayListener
) {
  val sliderValue = remember(viewState.playedTime.toDouble(DurationUnit.MILLISECONDS)) {
    mutableStateOf(viewState.playedTime.toDouble(DurationUnit.MILLISECONDS).toFloat())
  }
  Slider(
    value = sliderValue.value,
    valueRange = 0f..viewState.duration.toDouble(DurationUnit.MILLISECONDS).toFloat(),
    onValueChange = {
      sliderValue.value = it
    },
    onValueChangeFinished = {
      listener.seekTo(sliderValue.value.roundToLong())
    }
  )
}
