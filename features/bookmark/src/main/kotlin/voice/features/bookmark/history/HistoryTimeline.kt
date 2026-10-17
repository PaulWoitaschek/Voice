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
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

/** Shorter interruptions, like navigation prompts, would crowd a drive. */
private val MIN_INTERRUPTION = 1.minutes

/** Shorter pauses, like making a coffee, show as a dotted line instead of a new card. */
private val SESSION_GAP = 30.minutes

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
      if (previous == null || (!playingBefore(event) && event.atMillis - previous.atMillis >= SESSION_GAP.inWholeMilliseconds)) {
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
        back != null -> {
          val last = rows.lastOrNull()
          if (last is HistorySkips && last.back == back) {
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

/** Null unless it's a tap on rewind or fast forward, or a seek too small to undo. */
private fun ListeningEvent.skipsBack(): Boolean? = when {
  type == Type.SkipBack -> true
  type == Type.SkipForward -> false
  type == Type.Seek && !isUndoableJump() -> (toTime ?: time) < time
  else -> null
}

private fun ListeningEvent.isHeldBackUntil(next: ListeningEvent): Boolean {
  return type == Type.Pause && source == Source.AudioFocus && next.type == Type.Play && next.source == Source.AudioFocus
}
