package voice.features.bookmark.history

import androidx.compose.runtime.Immutable
import voice.core.data.ChapterId
import voice.core.data.ListeningEvent
import voice.features.bookmark.DayLabel
import java.time.LocalTime
import kotlin.time.Duration

/** The stretches of listening, newest first, under the part of the day they started in. */
@Immutable
internal data class HistoryViewState(val days: List<HistoryDay>)

@Immutable
internal data class HistoryDay(
  val label: DayLabel,
  val stretches: List<HistoryStretch>,
)

/**
 * Where something happened. [chapterNumber] is null when the book has a single chapter.
 */
@Immutable
internal data class HistoryLocation(
  val chapterNumber: Int?,
  val time: String,
)

/**
 * Listening from [from] to [to]. A pause of 15 minutes or more, or a big jump, starts the next one.
 *
 * [end], [listened] and [to] are null for a night that only a sleep timer bookmark remembered, from
 * before the history.
 */
@Immutable
internal data class HistoryStretch(
  val key: Long,
  val start: LocalTime,
  val end: LocalTime?,
  val listened: Duration?,
  val from: HistoryLocation,
  val to: HistoryLocation?,
  val cause: StretchCause,
  /** Null while it's still playing, or when the spot is gone after a rescan. */
  val goTo: HistorySpot?,
)

@Immutable
internal sealed interface StretchCause {

  data class Played(val source: ListeningEvent.Source) : StretchCause

  /** A big jump, the chapter list or a bookmark started it. */
  data class Jumped(
    val type: ListeningEvent.Type,
    val source: ListeningEvent.Source,
  ) : StretchCause

  data object SleepTimerEnded : StretchCause

  data object SleepTimerStarted : StretchCause
}

@Immutable
internal data class HistorySpot(
  val chapterId: ChapterId,
  val time: Long,
)
