package voice.features.bookmark

import voice.core.data.Bookmark
import voice.core.data.ChapterId
import voice.core.data.KioskModeDemoData
import voice.core.data.ListeningEvent.Source
import voice.core.data.ListeningEvent.Type
import voice.core.ui.BookBarPin
import voice.features.bookmark.history.HistoryDay
import voice.features.bookmark.history.HistoryJump
import voice.features.bookmark.history.HistoryLocation
import voice.features.bookmark.history.HistorySpot
import voice.features.bookmark.history.HistoryStretch
import voice.features.bookmark.history.HistoryTarget
import voice.features.bookmark.history.HistoryViewState
import voice.features.bookmark.history.StretchCause
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import kotlin.time.Duration
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
) = BookmarkRowViewState(
  id = demoId(index),
  kind = kind,
  note = note,
  chapterNumber = chapterNumber,
  chapterName = chapterName,
  time = time,
  percent = percent,
  savedAt = savedAt,
  showChapter = false,
)

private fun single(row: BookmarkRowViewState) = BookmarkListItem.Row(row, GroupPosition(0, 1))

internal fun kioskModeBookmarkViewState(tab: BookmarkTab): BookmarkViewState {
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
        BookBarPin(0.15F, Bookmark.Kind.Favorite),
        BookBarPin(0.46F, Bookmark.Kind.Quote),
        BookBarPin(0.65F, Bookmark.Kind.Revisit),
        BookBarPin(0.83F, Bookmark.Kind.Note),
      ),
      pinKeys = listOf(firstSignal, theory, message, later).map { it.id.value.toString() },
    ),
    sort = BookmarkSort.Story,
    categories = listOf(
      CategoryCount(Bookmark.Kind.Note, 1),
      CategoryCount(Bookmark.Kind.Favorite, 1),
      CategoryCount(Bookmark.Kind.Quote, 1),
      CategoryCount(Bookmark.Kind.Revisit, 1),
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
      BookmarkListItem.YouAreHere(time = "34:18", percent = 72, group = GroupPosition(0, 1)),
      BookmarkListItem.ChapterHeader(14, "Static"),
      single(later),
    ),
    totalCount = 4,
    editor = null,
    tab = tab,
  )
}

private fun at(
  chapterNumber: Int,
  time: String,
) = HistoryLocation(chapterNumber, time)

private fun demoStretch(
  key: Long,
  start: LocalTime,
  end: LocalTime,
  listened: Duration,
  from: HistoryLocation,
  to: HistoryLocation,
  cause: StretchCause,
) = HistoryStretch(
  key = key,
  start = start,
  end = end,
  listened = listened,
  from = from,
  to = to,
  cause = cause,
  // after a night, the history goes to where the listener was still awake
  goTo = HistoryTarget(HistorySpot(demoChapter, 0), if (cause == StretchCause.SleepTimerEnded) from else to),
)

internal fun kioskModeHistoryViewState(): HistoryViewState = HistoryViewState(
  days = listOf(
    HistoryDay(
      label = DayLabel.ThisMorning,
      rows = listOf(
        demoStretch(
          key = 4,
          start = LocalTime.of(7, 52),
          end = LocalTime.of(8, 11),
          listened = 19.minutes,
          from = at(12, "14:48"),
          to = at(12, "34:18"),
          cause = StretchCause.Played(Source.Car),
        ),
      ),
    ),
    HistoryDay(
      label = DayLabel.LastNight,
      rows = listOf(
        demoStretch(
          key = 3,
          start = LocalTime.of(22, 10),
          end = LocalTime.of(23, 42),
          listened = 89.minutes,
          from = at(10, "21:18"),
          to = at(12, "14:48"),
          cause = StretchCause.SleepTimerEnded,
        ),
      ),
    ),
    HistoryDay(
      label = DayLabel.Yesterday,
      rows = listOf(
        demoStretch(
          key = 2,
          start = LocalTime.of(18, 20),
          end = LocalTime.of(18, 24),
          listened = 4.minutes,
          from = at(15, "2:00"),
          to = at(15, "6:10"),
          cause = StretchCause.Jumped(Type.Seek, Source.Notification, hasRow = true),
        ),
        HistoryJump(
          key = 2,
          at = LocalTime.of(18, 20),
          from = at(10, "21:18"),
          to = at(15, "2:00"),
          type = Type.Seek,
          source = Source.Notification,
          back = HistoryTarget(HistorySpot(demoChapter, 0), at(10, "21:18")),
        ),
        demoStretch(
          key = 1,
          start = LocalTime.of(17, 40),
          end = LocalTime.of(18, 20),
          listened = 40.minutes,
          from = at(9, "5:00"),
          to = at(10, "21:18"),
          cause = StretchCause.Played(Source.Headset),
        ),
      ),
    ),
  ),
)
