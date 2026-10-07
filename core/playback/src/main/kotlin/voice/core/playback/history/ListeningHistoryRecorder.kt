package voice.core.playback.history

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import voice.core.data.BookId
import voice.core.data.ChapterId
import voice.core.data.ListeningEvent
import voice.core.data.repo.ListeningHistoryRepo
import java.time.Clock
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.absoluteValue
import kotlin.time.Duration.Companion.seconds

data class PlaybackPosition(
  val bookId: BookId,
  val chapterId: ChapterId,
  val time: Long,
)

/**
 * A jump that can be undone from the player.
 */
data class Jump(
  val from: PlaybackPosition,
  val to: PlaybackPosition?,
  val type: ListeningEvent.Type,
  val at: Instant,
)

@SingleIn(AppScope::class)
@Inject
class ListeningHistoryRecorder(
  private val repo: ListeningHistoryRepo,
  private val clock: Clock,
  private val scope: CoroutineScope,
) {

  val lastJump: StateFlow<Jump?>
    field = MutableStateFlow<Jump?>(null)

  private val pendingSettings = mutableMapOf<Pair<BookId, ListeningEvent.Type>, Job>()

  private val writes = Mutex()

  // bumped when the history is cleared, so that nothing recorded before is written after it
  private val generation = AtomicInteger()

  fun record(
    type: ListeningEvent.Type,
    source: ListeningEvent.Source,
    position: PlaybackPosition,
    to: PlaybackPosition? = null,
    value: String? = null,
    sourcePackage: String? = null,
  ) {
    val event = ListeningEvent(
      bookId = position.bookId,
      type = type,
      source = source,
      atMillis = clock.millis(),
      chapterId = position.chapterId,
      time = position.time,
      toChapterId = to?.chapterId,
      toTime = to?.time,
      value = value,
      sourcePackage = sourcePackage,
    )
    updateLastJump(event, position, to)
    val generation = generation.get()
    if (type in debouncedTypes) {
      // dragging a slider changes the value many times, only the last one matters
      val key = position.bookId to type
      pendingSettings.remove(key)?.cancel()
      pendingSettings[key] = scope.launch {
        delay(SETTINGS_DEBOUNCE)
        pendingSettings.remove(key)
        write(event, generation)
      }
    } else {
      scope.launch {
        write(event, generation)
      }
    }
  }

  /**
   * Clears the history, including what was recorded but is not written yet.
   */
  suspend fun clear() {
    writes.withLock {
      generation.incrementAndGet()
      lastJump.value = null
      repo.clear()
    }
  }

  private suspend fun write(
    event: ListeningEvent,
    generation: Int,
  ) {
    writes.withLock {
      if (generation == this.generation.get()) {
        repo.add(event)
      }
    }
  }

  /**
   * Clears [jump] unless a newer jump replaced it in the meantime.
   */
  fun clearJump(jump: Jump) {
    lastJump.compareAndSet(jump, null)
  }

  private fun updateLastJump(
    event: ListeningEvent,
    from: PlaybackPosition,
    to: PlaybackPosition?,
  ) {
    when {
      event.type == ListeningEvent.Type.JumpBack -> lastJump.value = null
      event.isUndoableJump() -> lastJump.value = Jump(
        from = from,
        to = to,
        type = event.type,
        at = Instant.ofEpochMilli(event.atMillis),
      )
    }
  }

  private companion object {
    val SETTINGS_DEBOUNCE = 2.seconds
    val debouncedTypes = setOf(
      ListeningEvent.Type.SpeedChanged,
      ListeningEvent.Type.VolumeBoostChanged,
    )
  }
}

private val minimumJump = 30.seconds

/**
 * Seeks of more than 30 seconds, chapter changes and bookmark jumps can be undone with
 * the back pill on the player. The rewind and fast forward buttons move only a few
 * seconds on purpose, so they can't.
 */
internal fun ListeningEvent.isUndoableJump(): Boolean {
  return when (type) {
    ListeningEvent.Type.ChapterChange,
    ListeningEvent.Type.BookmarkJump,
    -> true
    ListeningEvent.Type.Seek -> {
      val toTime = toTime
      toChapterId != chapterId || toTime == null || (toTime - time).absoluteValue >= minimumJump.inWholeMilliseconds
    }
    else -> false
  }
}
