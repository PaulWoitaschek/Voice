package voice.playbackScreen.views

import androidx.compose.material.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import de.ph1b.audiobook.features.bookPlaying.BookPlayViewState
import voice.playbackScreen.BookPlayListener
import kotlin.math.roundToLong

@Composable
internal fun PlaybackSlider(
  viewState: BookPlayViewState,
  listener: BookPlayListener
) {
  val sliderValue = remember(viewState.playedTime.inMilliseconds) {
    mutableStateOf(viewState.playedTime.inMilliseconds.toFloat())
  }
  Slider(
    value = sliderValue.value,
    valueRange = 0f..viewState.duration.inMilliseconds.toFloat(),
    onValueChange = {
      sliderValue.value = it
    },
    onValueChangeFinished = {
      listener.seekTo(sliderValue.value.roundToLong())
    }
  )
}
