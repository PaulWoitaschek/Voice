package voice.features.bookmark.history

import androidx.compose.runtime.Immutable
import voice.core.data.ChapterId
import voice.core.data.ListeningEvent
import voice.features.bookmark.DayLabel
import java.time.LocalTime
import kotlin.time.Duration

/** The stretches of listening and the jumps between them, newest first, under the part of the day they happened in. */
@Immutable
internal data class HistoryViewState(val days: List<HistoryDay>)

@Immutable
internal data class HistoryDay(
  val label: DayLabel,
  val rows: List<HistoryRow>,
)

/**
 * Where something happened. [chapterNumber] is null when the book has a single chapter.
 */
@Immutable
internal data class HistoryLocation(
  val chapterNumber: Int?,
  val time: String,
)

@Immutable
internal sealed interface HistoryRow {
  val key: Long
}

/**
 * Listening from [from] to [to]. A pause of 15 minutes or more, or a big jump, starts the next one.
 *
 * [end], [listened] and [to] are null for a night that only a sleep timer bookmark remembered, from
 * before the history.
 */
@Immutable
internal data class HistoryStretch(
  override val key: Long,
  val start: LocalTime,
  val end: LocalTime?,
  val listened: Duration?,
  val from: HistoryLocation,
  val to: HistoryLocation?,
  val cause: StretchCause,
  /** Null while it's still playing, or when the spot is gone after a rescan. */
  val goTo: HistoryTarget?,
) : HistoryRow

/**
 * A big jump, or several without listening in between, so an accidental one can be undone long
 * after the pill on the player is gone. [back] is where it left.
 *
 * [to] is null when the spot is gone after a rescan.
 */
@Immutable
internal data class HistoryJump(
  override val key: Long,
  val at: LocalTime,
  val from: HistoryLocation,
  val to: HistoryLocation?,
  val type: ListeningEvent.Type,
  val source: ListeningEvent.Source,
  val back: HistoryTarget,
) : HistoryRow

@Immutable
internal sealed interface StretchCause {

  data class Played(val source: ListeningEvent.Source) : StretchCause

  /**
   * A big jump, the chapter list or a bookmark started it. [hasRow] when the jump has a row of its
   * own, which already says so.
   */
  data class Jumped(
    val type: ListeningEvent.Type,
    val source: ListeningEvent.Source,
    val hasRow: Boolean,
  ) : StretchCause

  data object SleepTimerEnded : StretchCause

  data object SleepTimerStarted : StretchCause
}

/** Where a row's button goes, and how that place reads. */
@Immutable
internal data class HistoryTarget(
  val spot: HistorySpot,
  val location: HistoryLocation,
)

@Immutable
internal data class HistorySpot(
  val chapterId: ChapterId,
  val time: Long,
)
