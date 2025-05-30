package voice.playbackScreen

import androidx.compose.runtime.Immutable
import voice.common.compose.ImmutableFile
import voice.playback.misc.Decibel
import voice.sleepTimer.SleepTimerViewState
import kotlin.time.Duration

@Immutable
data class BookPlayViewState(
  val chapterName: String?,
  val showPreviousNextButtons: Boolean,
  val title: String,
  val sleepTime: Duration,
  val playedTime: Duration,
  val duration: Duration,
  val playing: Boolean,
  val cover: ImmutableFile?,
  val skipSilence: Boolean,
) {

  init {
    require(duration > Duration.ZERO) {
      "Duration must be positive in $this"
    }
  }
}

internal sealed interface BookPlayDialogViewState {
  data class SpeedDialog(val speed: Float) : BookPlayDialogViewState {

    val maxSpeed: Float get() = if (speed < 2F) 2F else 3.5F
  }

  data class VolumeGainDialog(
    val gain: Decibel,
    val valueFormatted: String,
    val maxGain: Decibel,
  ) : BookPlayDialogViewState

  data class SelectChapterDialog(val items: List<ItemViewState>) : BookPlayDialogViewState {

    data class ItemViewState(
      val number: Int,
      val name: String,
      val active: Boolean,
    )
  }

  @JvmInline
  value class SleepTimer(val viewState: SleepTimerViewState) : BookPlayDialogViewState
}
