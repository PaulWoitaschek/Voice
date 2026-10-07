package voice.features.playbackScreen

import androidx.compose.runtime.Immutable
import voice.core.playback.misc.Decibel
import voice.core.ui.BookBarPin
import voice.features.sleepTimer.SleepTimerViewState
import kotlin.time.Duration

@Immutable
data class BookPlayViewState(
  val chapterName: String?,
  val showPreviousNextButtons: Boolean,
  val title: String,
  val author: String?,
  val sleepTimerState: SleepTimerViewState,
  val playedTime: Duration,
  val duration: Duration,
  val playing: Boolean,
  val cover: String?,
  val skipSilence: Boolean,
  val playbackSpeed: Float,
  val volumeBoostActive: Boolean,
  val chapterNumber: Int,
  val chapterCount: Int,
  val bookPlayedTime: Duration,
  val bookDuration: Duration,
  val chapterSegments: List<Float>,
  val skipSeconds: Int,
  val bookmarkPins: List<BookBarPin>,
  val poppedPin: Int?,
  val jumpBack: JumpBackViewState?,
) {

  /**
   * Offers to undo the last big jump for a few seconds.
   *
   * @param id identifies the jump, a new jump restarts the countdown
   * @param chapterNumber set if the jump left the chapter
   */
  @Immutable
  data class JumpBackViewState(
    val id: Long,
    val time: String,
    val chapterNumber: Int?,
    val remaining: Duration,
    val visibleFor: Duration,
  )

  val bookProgress: Float
    get() = if (bookDuration > Duration.ZERO) {
      (bookPlayedTime / bookDuration).toFloat().coerceIn(0F, 1F)
    } else {
      0F
    }

  sealed interface SleepTimerViewState {
    data object Disabled : SleepTimerViewState

    sealed interface Enabled : SleepTimerViewState {
      data object WithEndOfChapter : Enabled

      @JvmInline
      value class WithDuration(val leftDuration: Duration) : Enabled
    }
  }

  init {
    require(duration > Duration.ZERO) {
      "Duration must be positive in $this"
    }
  }
}

internal sealed interface BookPlayDialogViewState {
  data class SpeedDialog(val speed: Float) : BookPlayDialogViewState

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
      val time: String,
    )
  }

  @JvmInline
  value class SleepTimer(val viewState: SleepTimerViewState) : BookPlayDialogViewState
}
