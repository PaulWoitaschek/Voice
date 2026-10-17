package voice.features.bookmark.history

import androidx.compose.runtime.Immutable
import voice.core.data.ChapterId
import voice.core.data.ListeningEvent
import voice.features.bookmark.DayLabel
import java.time.LocalTime
import kotlin.time.Duration

/** What happened while listening, under the part of the day each listening session started in, newest first. */
@Immutable
internal data class HistoryViewState(val days: List<HistoryDay>)

@Immutable
internal data class HistoryDay(
  val label: DayLabel,
  val sessions: List<HistorySession>,
)

@Immutable
internal data class HistorySession(val rows: List<HistoryRow>) {
  val key: Long get() = rows.last().key
}

@Immutable
internal sealed interface HistoryRow {
  val key: Long
  val at: LocalTime

  /** Whether the book played from the row that happened before this one until this one. */
  val playingBefore: Boolean
}

/** Something that happened at [place]. A jump also has [to], unless that spot is gone after a rescan. */
@Immutable
internal data class HistoryEvent(
  override val key: Long,
  override val at: LocalTime,
  override val playingBefore: Boolean,
  val type: ListeningEvent.Type,
  val source: ListeningEvent.Source,
  val place: HistoryPlace,
  val to: HistoryPlace? = null,
) : HistoryRow

@Immutable
internal data class HistorySkips(
  override val key: Long,
  override val at: LocalTime,
  override val playingBefore: Boolean,
  val count: Int,
  val back: Boolean,
) : HistoryRow

@Immutable
internal data class HistoryInterruption(
  override val key: Long,
  override val at: LocalTime,
  override val playingBefore: Boolean,
  val duration: Duration,
) : HistoryRow

/** A place in the book a row can go to, and how it reads. */
@Immutable
internal data class HistoryPlace(
  val spot: HistorySpot,
  val location: HistoryLocation,
)

@Immutable
internal data class HistorySpot(
  val chapterId: ChapterId,
  val time: Long,
)

/**
 * Where something happened. [chapterNumber] is null when the book has a single chapter.
 */
@Immutable
internal data class HistoryLocation(
  val chapterNumber: Int?,
  val time: String,
)
