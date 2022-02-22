package voice.playbackScreen

import java.io.File
import kotlin.time.Duration
import androidx.annotation.FloatRange

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
  val bookDuration: Duration,
  val bookPlayedTime: Duration,
  @FloatRange(from = 0.0, to = 1.0)
  val bookProgress: Float,
)
