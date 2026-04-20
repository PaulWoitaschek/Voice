package voice.core.ui

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.runtime.withFrameMillis
import voice.core.data.Book
import kotlin.time.AbstractLongTimeSource
import kotlin.time.DurationUnit

@Composable
fun rememberProgressingBook(book: Book): State<Book> {
  return produceState(
    initialValue = book,
    book,
  ) {
    val startedAt = RealtimeMonotonicTimeSource.markNow()
    while (true) {
      withFrameMillis {
        val elapsedSinceStart = startedAt.elapsedNow().inWholeMilliseconds
        value = book.withElapsedPosition(
          elapsedTime = elapsedSinceStart,
          playbackSpeed = book.content.playbackSpeed,
        )
      }
    }
  }
}

private object RealtimeMonotonicTimeSource : AbstractLongTimeSource(DurationUnit.NANOSECONDS) {
  override fun read(): Long = SystemClock.elapsedRealtimeNanos()
}
