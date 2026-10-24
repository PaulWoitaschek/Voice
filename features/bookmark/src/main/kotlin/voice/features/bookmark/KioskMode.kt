package voice.features.bookmark

import voice.core.data.Bookmark
import voice.core.data.ChapterId
import voice.core.data.ListeningEvent.Source
import voice.core.data.ListeningEvent.Type
import voice.core.kioskmode.KioskModeDemoData
import voice.core.ui.BookBarPin
import voice.features.bookmark.history.HistoryDay
import voice.features.bookmark.history.HistoryEvent
import voice.features.bookmark.history.HistoryInterruption
import voice.features.bookmark.history.HistoryLocation
import voice.features.bookmark.history.HistoryPlace
import voice.features.bookmark.history.HistorySession
import voice.features.bookmark.history.HistorySkips
import voice.features.bookmark.history.HistorySpot
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

private fun place(
  chapterNumber: Int,
  time: String,
) = HistoryPlace(HistorySpot(demoChapter, 0), HistoryLocation(chapterNumber, time))

private fun event(
  key: Long,
  at: LocalTime,
  playingBefore: Boolean,
  type: Type,
  place: HistoryPlace,
  source: Source = Source.App,
  to: HistoryPlace? = null,
) = HistoryEvent(key = key, at = at, playingBefore = playingBefore, type = type, source = source, place = place, to = to)

internal fun kioskModeHistoryViewState(): HistoryViewState = HistoryViewState(
  days = listOf(
    HistoryDay(
      label = DayLabel.ThisMorning,
      sessions = listOf(
        HistorySession(
          rows = listOf(
            event(14, LocalTime.of(8, 15), playingBefore = true, Type.Pause, place(12, "34:18"), Source.Car),
            HistoryInterruption(key = 13, at = LocalTime.of(8, 1), playingBefore = true, duration = 3.minutes),
            event(12, LocalTime.of(7, 52), playingBefore = false, Type.Play, place(12, "14:40"), Source.Car),
          ),
        ),
      ),
    ),
    HistoryDay(
      label = DayLabel.LastNight,
      sessions = listOf(
        HistorySession(
          rows = listOf(
            event(11, LocalTime.of(23, 10), playingBefore = true, Type.SleepTimerEnded, place(12, "14:48"), Source.SleepTimer),
            event(10, LocalTime.of(22, 40), playingBefore = false, Type.SleepTimerExtended, place(11, "31:58"), Source.SleepTimer),
            event(9, LocalTime.of(22, 40), playingBefore = true, Type.SleepTimerEnded, place(11, "32:10"), Source.SleepTimer),
            event(8, LocalTime.of(22, 10), playingBefore = true, Type.SleepTimerStarted, place(11, "2:10")),
            event(7, LocalTime.of(22, 10), playingBefore = false, Type.Play, place(11, "2:10")),
          ),
        ),
      ),
    ),
    HistoryDay(
      label = DayLabel.Yesterday,
      sessions = listOf(
        HistorySession(
          rows = listOf(
            event(6, LocalTime.of(17, 45), playingBefore = true, Type.Pause, place(11, "2:10"), Source.Headset),
            event(5, LocalTime.of(17, 21), playingBefore = true, Type.JumpBack, place(15, "2:40"), to = place(10, "21:18")),
            event(
              4,
              LocalTime.of(17, 20),
              playingBefore = true,
              Type.Seek,
              place(10, "21:18"),
              Source.Notification,
              to = place(15, "2:00"),
            ),
            HistorySkips(key = 2, at = LocalTime.of(17, 2), playingBefore = true, count = 2, back = true),
            event(1, LocalTime.of(16, 40), playingBefore = false, Type.Play, place(9, "5:00"), Source.Headset),
          ),
        ),
        HistorySession(
          rows = listOf(
            event(16, LocalTime.of(8, 30), playingBefore = true, Type.Pause, place(9, "5:00"), Source.Car),
            event(15, LocalTime.of(8, 5), playingBefore = false, Type.Play, place(8, "31:12"), Source.Car),
          ),
        ),
      ),
    ),
  ),
)
