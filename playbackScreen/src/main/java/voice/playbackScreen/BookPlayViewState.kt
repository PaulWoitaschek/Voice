package de.ph1b.audiobook.features.bookPlaying

import android.content.Context
import de.ph1b.audiobook.common.CoverReplacement
import de.ph1b.audiobook.data.Book
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
  val cover: BookPlayCover,
  val skipSilence: Boolean
)

data class BookPlayCover(private val book: Book) {

  fun file(context: Context): File {
    return book.coverFile(context)
  }

  fun placeholder(context: Context): CoverReplacement = CoverReplacement(book.name, context)

  fun coverTransitionName(): String = book.coverTransitionName
}
