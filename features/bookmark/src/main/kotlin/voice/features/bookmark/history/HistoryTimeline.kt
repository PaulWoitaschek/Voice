package voice.features.bookmark.history

import voice.core.data.ChapterId
import voice.core.data.ListeningEvent
import voice.core.data.ListeningEvent.Source
import voice.core.data.ListeningEvent.Type
import voice.core.data.at
import voice.core.data.isUndoableJump
import voice.features.bookmark.BookIndex
import voice.features.bookmark.sessionLabel
import java.time.Instant
import java.time.ZoneId
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

/** Shorter interruptions, like navigation prompts, would crowd a drive. */
private val MIN_INTERRUPTION = 1.minutes

/** Shorter pauses, like making a coffee, show as a dotted line instead of a new card. */
private val SESSION_GAP = 30.minutes

/** Taps further apart show at their own time. */
private val SKIP_RUN = 1.minutes

private const val SLOWEST_SPEED = 0.5

internal fun historyViewState(
  events: List<ListeningEvent>,
  index: BookIndex,
  now: Instant,
  zone: ZoneId,
): HistoryViewState {
  val sessions = Timeline(index, zone)
    .sessions(events.sortedWith(compareBy({ it.atMillis }, { it.id })))
    .filter { it.rows.isNotEmpty() }
    .asReversed()
  return HistoryViewState(
    // each label covers one stretch of time, so grouping keeps the sessions in order
    sessions
      .groupBy { sessionLabel(it.start, now, zone) }
      .map { (label, inDay) -> HistoryDay(label, inDay.map { HistorySession(it.rows.reversed()) }) },
  )
}

private class Session(
  val start: Instant,
  val rows: MutableList<HistoryRow> = mutableListOf(),
)

private class Timeline(
  private val index: BookIndex,
  private val zone: ZoneId,
) {

  private var playing = false

  fun sessions(events: List<ListeningEvent>): List<Session> {
    val sessions = mutableListOf<Session>()
    var position = 0
    while (position < events.size) {
      val event = events[position]
      val previous = events.getOrNull(position - 1)
      val gap = if (previous == null) Duration.INFINITE else (event.atMillis - previous.atMillis).milliseconds
      // the pause gets lost when the app is killed while playing, and then the book stands still
      if (playing && previous != null && gap >= SESSION_GAP && !movedOn(previous, event, gap)) {
        playing = false
      }
      if (gap >= SESSION_GAP && !playingBefore(event)) {
        sessions += Session(event.at)
      }
      val rows = sessions.last().rows
      val next = events.getOrNull(position + 1)
      val back = event.skipsBack()
      when {
        next != null && event.isHeldBackUntil(next) -> {
          val duration = (next.atMillis - event.atMillis).milliseconds
          if (duration >= MIN_INTERRUPTION) {
            rows += HistoryInterruption(key = event.id, at = event.localTime(), playingBefore = playing, duration = duration)
          }
          playing = true
          position++
        }
        // syncing and going to a place from here make small seeks too, so they'd only be noise
        event.type == Type.Seek && !event.isUndoableJump() -> Unit
        back != null -> {
          val last = rows.lastOrNull()
          if (last is HistorySkips && last.back == back && gap < SKIP_RUN) {
            rows[rows.lastIndex] = last.copy(count = last.count + 1)
          } else {
            rows += HistorySkips(key = event.id, at = event.localTime(), playingBefore = playing, count = 1, back = back)
          }
        }
        else -> {
          val playingBefore = playingBefore(event)
          playing = when (event.type) {
            Type.Play, Type.SleepTimerExtended -> true
            Type.Pause, Type.SleepTimerEnded -> false
            else -> playing
          }
          val place = place(event.chapterId, event.time)
          if (place != null) {
            rows += HistoryEvent(
              key = event.id,
              at = event.localTime(),
              playingBefore = playingBefore,
              type = event.type,
              source = event.source,
              place = place,
              to = event.toChapterId?.let { place(it, event.toTime ?: 0L) },
            )
          }
        }
      }
      position++
    }
    return sessions
  }

  // a play always comes after a pause, even when the pause got lost because the app was killed
  private fun playingBefore(event: ListeningEvent): Boolean = playing && event.type != Type.Play

  private fun movedOn(
    previous: ListeningEvent,
    event: ListeningEvent,
    gap: Duration,
  ): Boolean {
    val from = previous.toChapterId?.let { index.locate(it, previous.toTime ?: 0L) }
      ?: index.locate(previous.chapterId, previous.time)
      ?: return true
    val to = index.locate(event.chapterId, event.time) ?: return true
    return to.bookPosition - from.bookPosition >= gap.inWholeMilliseconds * SLOWEST_SPEED
  }

  private fun place(
    chapterId: ChapterId,
    time: Long,
  ): HistoryPlace? {
    val location = index.locate(chapterId, time) ?: return null
    return HistoryPlace(
      spot = HistorySpot(chapterId, time),
      location = HistoryLocation(chapterNumber = location.chapterNumber.takeIf { index.chapterCount > 1 }, time = location.time),
    )
  }

  private fun ListeningEvent.localTime() = at.atZone(zone).toLocalTime().withSecond(0).withNano(0)
}

private fun ListeningEvent.skipsBack(): Boolean? = when (type) {
  Type.SkipBack -> true
  Type.SkipForward -> false
  else -> null
}

private fun ListeningEvent.isHeldBackUntil(next: ListeningEvent): Boolean {
  return type == Type.Pause && source == Source.AudioFocus && next.type == Type.Play && next.source == Source.AudioFocus
}
