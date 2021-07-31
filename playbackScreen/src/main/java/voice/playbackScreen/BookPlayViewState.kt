package voice.playbackScreen

import android.content.Context
import de.ph1b.audiobook.common.CoverReplacement
import de.ph1b.audiobook.data.bookCover
import java.io.File
import java.util.UUID
import kotlin.time.Duration

data class BookPlayViewState(
  val chapterName: String?,
  val showPreviousNextButtons: Boolean,
  val title: String,
  val sleepTime: Duration,
  val playedTime: Duration,
  val duration: Duration,
  val playing: Boolean,
  val cover: BookPlayCover,
  val skipSilence: Boolean
)

data class BookPlayCover(
  private val bookName: String,
  private val bookId: UUID,
) {

  fun file(context: Context): File {
    return bookCover(context, bookId)
  }

  fun placeholder(context: Context): CoverReplacement = CoverReplacement(bookName, context)
}
