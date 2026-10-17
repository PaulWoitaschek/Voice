package voice.features.bookmark.history

import voice.core.data.ChapterId
import voice.core.data.ListeningEvent
import voice.core.data.ListeningEvent.Source
import voice.core.data.ListeningEvent.Type
import voice.core.data.at
import voice.core.data.isUndoableJump
import voice.features.bookmark.BookIndex
import voice.features.bookmark.DayLabel
import voice.features.bookmark.sessionLabel
import java.time.Instant
import java.time.ZoneId
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

/** Shorter interruptions, like navigation prompts, would crowd a drive. */
private val MIN_INTERRUPTION = 1.minutes

internal fun historyViewState(
  events: List<ListeningEvent>,
  index: BookIndex,
  now: Instant,
  zone: ZoneId,
): HistoryViewState {
  val days = mutableListOf<Pair<DayLabel, MutableList<ListeningEvent>>>()
  events.sortedWith(compareBy({ it.atMillis }, { it.id })).forEach { event ->
    val label = sessionLabel(event.at, now, zone)
    val last = days.lastOrNull()
    if (last != null && last.first == label) {
      last.second += event
    } else {
      days += label to mutableListOf(event)
    }
  }
  val timeline = Timeline(index, zone)
  return HistoryViewState(
    days
      .map { (label, dayEvents) -> HistoryDay(label, timeline.rows(dayEvents)) }
      .filter { it.rows.isNotEmpty() }
      .reversed(),
  )
}

/** Goes through the days oldest first, because whether the book was playing carries over from one day to the next. */
private class Timeline(
  private val index: BookIndex,
  private val zone: ZoneId,
) {

  private var playing = false

  fun rows(events: List<ListeningEvent>): List<HistoryRow> {
    val rows = mutableListOf<HistoryRow>()
    var position = 0
    while (position < events.size) {
      val event = events[position]
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
          // a play always comes after a pause, even when the pause got lost because the app was killed
          val playingBefore = playing && event.type != Type.Play
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
    return rows
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
