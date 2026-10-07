package voice.features.bookmark

import voice.core.data.Bookmark
import voice.core.data.ChapterId
import voice.core.data.KioskModeDemoData
import voice.core.data.ListeningEvent.Source
import voice.core.data.ListeningEvent.Type
import voice.core.ui.BookBarPin
import voice.features.bookmark.history.HistoryAction
import voice.features.bookmark.history.HistoryDetail
import voice.features.bookmark.history.HistoryEntry
import voice.features.bookmark.history.HistoryFilter
import voice.features.bookmark.history.HistoryLocation
import voice.features.bookmark.history.HistorySession
import voice.features.bookmark.history.HistorySuggestion
import voice.features.bookmark.history.HistoryViewState
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import kotlin.time.Duration.Companion.minutes
import kotlin.uuid.Uuid

private val demoBook = KioskModeDemoData.echoesOfTomorrow
private val demoChapter = ChapterId("echoes_of_tomorrow")

private fun demoId(index: Int) = Bookmark.Id(Uuid.parse("00000000-0000-0000-0000-00000000000$index"))

private fun demoRow(
  index: Int,
  kind: Bookmark.Kind,
  note: String?,
  chapterNumber: Int,
  chapterName: String,
  time: String,
  percent: Int,
  savedAt: DayLabel,
  setBySleepTimer: Boolean = false,
) = BookmarkRowViewState(
  id = demoId(index),
  kind = kind,
  setBySleepTimer = setBySleepTimer,
  note = note,
  chapterNumber = chapterNumber,
  chapterName = chapterName,
  time = time,
  percent = percent,
  savedAt = savedAt,
  showChapter = false,
)

private fun single(row: BookmarkRowViewState) = BookmarkListItem.Row(row, GroupPosition(0, 1))

internal fun kioskModeBookmarkViewState(): BookmarkViewState {
  val firstSignal =
    demoRow(
      1,
      Bookmark.Kind.Favorite,
      "The first signal",
      3,
      "Static on the Line",
      "12:40",
      15,
      DayLabel.Date(LocalDate.of(2026, 9, 12), withYear = false),
    )
  val theory = demoRow(2, Bookmark.Kind.Quote, "Mira's theory", 8, "Mira", "24:31", 46, DayLabel.Weekday(DayOfWeek.THURSDAY))
  val message = demoRow(3, Bookmark.Kind.Revisit, "The hidden message", 11, "Interference", "21:48", 65, DayLabel.Yesterday)
  val dozedOff = demoRow(4, Bookmark.Kind.Note, null, 12, "The Signal", "14:48", 70, DayLabel.LastNight, setBySleepTimer = true)
  val later =
    demoRow(
      5,
      Bookmark.Kind.Note,
      "Return to this later",
      14,
      "Static",
      "36:44",
      83,
      DayLabel.Date(LocalDate.of(2026, 9, 30), withYear = false),
    )
  val segments = listOf(4F, 6F, 5F, 7F, 5F, 6F, 4F, 7F, 6F, 5F, 6F, 7F, 5F, 6F, 4F, 5F, 6F, 6F)
  return BookmarkViewState(
    bookTitle = demoBook.title,
    bookAuthor = demoBook.author,
    cover = demoBook.coverUrl,
    playing = true,
    sleepTimerActive = false,
    bookBar = BookBarViewState(
      segments = segments.map { it / segments.sum() },
      currentSegment = 11,
      progress = 0.72F,
      percent = 72,
      remaining = demoBook.remaining,
      pins = listOf(
        BookBarPin(0.15F, Bookmark.Kind.Favorite, setBySleepTimer = false),
        BookBarPin(0.46F, Bookmark.Kind.Quote, setBySleepTimer = false),
        BookBarPin(0.65F, Bookmark.Kind.Revisit, setBySleepTimer = false),
        BookBarPin(0.70F, Bookmark.Kind.Note, setBySleepTimer = true),
        BookBarPin(0.83F, Bookmark.Kind.Note, setBySleepTimer = false),
      ),
      pinKeys = listOf(firstSignal, theory, message, dozedOff, later).map { it.id.value.toString() },
    ),
    sort = BookmarkSort.Story,
    categories = listOf(
      CategoryCount(BookmarkCategory.Favorite, 1),
      CategoryCount(BookmarkCategory.Quote, 1),
      CategoryCount(BookmarkCategory.Revisit, 1),
      CategoryCount(BookmarkCategory.Note, 1),
      CategoryCount(BookmarkCategory.Sleep, 1),
    ),
    selectedCategory = null,
    items = listOf(
      BookmarkListItem.ChapterHeader(3, "Static on the Line"),
      single(firstSignal),
      BookmarkListItem.ChapterHeader(8, "Mira"),
      single(theory),
      BookmarkListItem.ChapterHeader(11, "Interference"),
      single(message),
      BookmarkListItem.ChapterHeader(12, "The Signal"),
      BookmarkListItem.Row(dozedOff, GroupPosition(0, 2)),
      BookmarkListItem.YouAreHere(time = "34:18", percent = 72, group = GroupPosition(1, 2)),
      BookmarkListItem.ChapterHeader(14, "Static"),
      single(later),
    ),
    totalCount = 5,
    editor = null,
  )
}

private fun at(
  chapterNumber: Int,
  time: String,
) = HistoryLocation(chapterNumber, time)

private fun demoEntry(
  key: Long,
  at: LocalTime,
  type: Type,
  source: Source,
  where: HistoryLocation,
  count: Int = 1,
  detail: HistoryDetail? = null,
  lastTouch: Boolean = false,
  action: HistoryAction? = HistoryAction.Pin(demoChapter, 0),
) = HistoryEntry(
  key = key,
  at = at,
  type = type,
  source = source,
  count = count,
  where = where,
  to = null,
  detail = detail,
  lastTouch = lastTouch,
  action = action,
)

internal fun kioskModeHistoryViewState(): HistoryViewState {
  val backToStart = HistoryAction.JumpBack(demoChapter, 0, at(12, "14:48"))
  return HistoryViewState(
    enabled = true,
    suggestion = HistorySuggestion.StartedBy(
      key = 7,
      back = backToStart,
      source = Source.Car,
      at = LocalTime.of(7, 52),
      played = 19.minutes,
    ),
    filters = listOf(HistoryFilter.Jumps, HistoryFilter.PlayPause, HistoryFilter.Sleep),
    selectedFilter = null,
    sources = listOf(Source.App, Source.Headset, Source.Car, Source.AudioFocus, Source.SleepTimer),
    selectedSource = null,
    sessions = listOf(
      HistorySession(
        key = 7,
        label = DayLabel.ThisMorning,
        start = LocalTime.of(7, 52),
        end = LocalTime.of(8, 11),
        listened = 19.minutes,
        barStart = 0.70F,
        barEnd = 0.72F,
        entries = listOf(
          demoEntry(8, LocalTime.of(8, 11), Type.Pause, Source.Car, at(12, "34:18")),
          demoEntry(7, LocalTime.of(7, 52), Type.Play, Source.Car, at(12, "14:48"), action = backToStart),
        ),
      ),
      HistorySession(
        key = 1,
        label = DayLabel.LastNight,
        start = LocalTime.of(22, 10),
        end = LocalTime.of(23, 42),
        listened = 89.minutes,
        barStart = 0.63F,
        barEnd = 0.70F,
        entries = listOf(
          demoEntry(6, LocalTime.of(23, 42), Type.SleepTimerEnded, Source.SleepTimer, at(12, "14:48")),
          demoEntry(
            key = 5,
            at = LocalTime.of(23, 12),
            type = Type.SleepTimerSet,
            source = Source.App,
            where = at(11, "31:48"),
            detail = HistoryDetail.SleepTimer(30),
            lastTouch = true,
            action = HistoryAction.GoThere(demoChapter, 0),
          ),
          demoEntry(
            key = 3,
            at = LocalTime.of(22, 51),
            type = Type.SkipForward,
            source = Source.App,
            where = at(11, "9:18"),
            count = 3,
            detail = HistoryDetail.Skipped(90),
            action = HistoryAction.JumpBack(demoChapter, 0, at(11, "9:18")),
          ),
          demoEntry(
            key = 2,
            at = LocalTime.of(22, 48),
            type = Type.Pause,
            source = Source.AudioFocus,
            where = at(11, "9:18"),
            detail = HistoryDetail.PausedFor(3.minutes),
          ),
          demoEntry(1, LocalTime.of(22, 10), Type.Play, Source.Headset, at(10, "21:18")),
        ),
      ),
    ),
    hasEvents = true,
  )
}
