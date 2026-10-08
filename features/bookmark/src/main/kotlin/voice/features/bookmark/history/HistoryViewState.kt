package voice.features.bookmark.history

import androidx.compose.runtime.Immutable
import voice.core.data.Bookmark
import voice.core.data.ChapterId
import voice.core.data.ListeningEvent
import voice.features.bookmark.DayLabel
import java.time.LocalTime
import kotlin.time.Duration

@Immutable
internal data class HistoryViewState(
  val suggestion: HistorySuggestion?,
  val filters: List<HistoryFilter>,
  val selectedFilter: HistoryFilter?,
  val sources: List<ListeningEvent.Source>,
  val selectedSource: ListeningEvent.Source?,
  val sessions: List<HistorySession>,
  val hasEvents: Boolean,
)

internal enum class HistoryFilter {
  Jumps,
  PlayPause,
  Sleep,
  Bookmarks,
  Settings,
}

/**
 * Where something happened. [chapterNumber] is null when the book has a single chapter.
 */
@Immutable
internal data class HistoryLocation(
  val chapterNumber: Int?,
  val time: String,
)

/**
 * Listening without a pause of 15 minutes or more.
 *
 * @param barStart where in the book the session started, from 0 to 1.
 * @param barEnd where in the book the session ended, from 0 to 1.
 */
@Immutable
internal data class HistorySession(
  val key: Long,
  val label: DayLabel,
  val start: LocalTime,
  val end: LocalTime,
  val listened: Duration,
  val barStart: Float,
  val barEnd: Float,
  val entries: List<HistoryEntry>,
)

/**
 * One row of the history. Repeats close to each other are merged into one entry, [count] says
 * how many.
 */
@Immutable
internal data class HistoryEntry(
  val key: Long,
  val at: LocalTime,
  val type: ListeningEvent.Type,
  val source: ListeningEvent.Source,
  val count: Int,
  val where: HistoryLocation?,
  val to: HistoryLocation?,
  val detail: HistoryDetail?,
  val lastTouch: Boolean,
  val action: HistoryAction?,
)

/** What changed, beyond the type and where. */
@Immutable
internal sealed interface HistoryDetail {

  /** A pause that playback resumed from, after [duration]. */
  data class PausedFor(val duration: Duration) : HistoryDetail

  /** Skips, in total seconds. */
  data class Skipped(val seconds: Int) : HistoryDetail

  data class Speed(
    val from: Float?,
    val to: Float,
  ) : HistoryDetail

  data class VolumeBoost(
    val from: Float?,
    val to: Float,
  ) : HistoryDetail

  data class SkipSilence(val enabled: Boolean) : HistoryDetail

  /** [minutes] is null for the end of the chapter. */
  data class SleepTimer(val minutes: Int?) : HistoryDetail

  data class BookmarkInfo(
    val kind: Bookmark.Kind,
    val setBySleepTimer: Boolean,
    val note: String?,
  ) : HistoryDetail
}

@Immutable
internal sealed interface HistoryAction {

  /** Undoes a jump, or an accidental play, by going back to where playback was. */
  data class JumpBack(
    val chapterId: ChapterId,
    val time: Long,
    val location: HistoryLocation,
  ) : HistoryAction

  /** Goes to the last spot touched before falling asleep. */
  data class GoThere(
    val chapterId: ChapterId,
    val time: Long,
  ) : HistoryAction

  data class Pin(
    val chapterId: ChapterId,
    val time: Long,
  ) : HistoryAction

  data class Restore(val bookmark: Bookmark) : HistoryAction

  data class ChangeBack(
    val type: ListeningEvent.Type,
    val value: String,
  ) : HistoryAction
}

/**
 * Something in the history that looks like an accident, offered on top with a way back.
 */
@Immutable
internal sealed interface HistorySuggestion {
  val key: Long
  val back: HistoryAction.JumpBack

  data class StartedBy(
    override val key: Long,
    override val back: HistoryAction.JumpBack,
    val source: ListeningEvent.Source,
    val at: LocalTime,
    val played: Duration,
  ) : HistorySuggestion

  data class Jumped(
    override val key: Long,
    override val back: HistoryAction.JumpBack,
    val at: LocalTime,
  ) : HistorySuggestion
}
