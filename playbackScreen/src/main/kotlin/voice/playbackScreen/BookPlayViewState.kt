package voice.playbackScreen

import java.io.File
import kotlin.time.Duration

data class BookPlayViewState(
  val chapterName: String?,
  val showPreviousNextButtons: Boolean,
  val title: String,
  val sleepTime: Duration,
  val playedTime: Duration,
  val duration: Duration,
  val playing: Boolean,
  val cover: File?,
  val skipSilence: Boolean
)
