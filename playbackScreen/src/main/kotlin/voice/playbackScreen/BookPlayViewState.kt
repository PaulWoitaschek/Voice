package voice.playbackScreen

import androidx.compose.runtime.Immutable
import voice.data.ChapterMark
import voice.playback.misc.Decibel
import java.io.File
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
  val cover: File?,
  val skipSilence: Boolean,
)

internal sealed interface BookPlayDialogViewState {
  data class SpeedDialog(
    val speed: Float,
  ) : BookPlayDialogViewState {

    val maxSpeed: Float get() = if (speed < 2F) 2F else 3.5F
  }

  data class VolumeGainDialog(
    val gain: Decibel,
    val valueFormatted: String,
    val maxGain: Decibel,
  ) : BookPlayDialogViewState

  data class SelectChapterDialog(
    val chapters: List<ChapterMark>,
    val selectedIndex: Int?,
  ) : BookPlayDialogViewState
}
