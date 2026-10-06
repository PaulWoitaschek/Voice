package voice.core.sleeptimer

import androidx.datastore.core.DataStore
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.first
import voice.core.data.BookId
import voice.core.data.sleeptimer.SleepTimerRewind
import voice.core.data.store.CurrentBookStore
import voice.core.data.store.SleepTimerRewindStore
import voice.core.playback.CurrentBookResolver
import kotlin.time.Duration

/**
 * Remembers where a sleep timer countdown started and, once it ran out, stores a [SleepTimerRewind]
 * so the user is offered to go back to where they might have fallen asleep.
 */
@SingleIn(AppScope::class)
@Inject
class SleepTimerRewindRecorder(
  @SleepTimerRewindStore
  private val sleepTimerRewindStore: DataStore<SleepTimerRewind?>,
  @CurrentBookStore
  private val currentBookStore: DataStore<BookId?>,
  private val currentBookResolver: CurrentBookResolver,
) {

  private var countdownStart: CountdownStart? = null

  suspend fun onCountdownStarted() {
    val bookId = currentBookStore.data.first()
    val book = bookId?.let { currentBookResolver.book(it) }
    countdownStart = book?.let {
      CountdownStart(
        bookId = it.id,
        positionInBookMs = it.position,
        playbackSpeed = it.content.playbackSpeed,
      )
    }
  }

  suspend fun onCountdownFinished(timerDuration: Duration) {
    val start = countdownStart ?: return
    countdownStart = null
    sleepTimerRewindStore.updateData {
      SleepTimerRewind(
        bookId = start.bookId,
        startPositionInBookMs = start.positionInBookMs,
        timerDuration = timerDuration,
        playbackSpeed = start.playbackSpeed,
      )
    }
  }

  private data class CountdownStart(
    val bookId: BookId,
    val positionInBookMs: Long,
    val playbackSpeed: Float,
  )
}
