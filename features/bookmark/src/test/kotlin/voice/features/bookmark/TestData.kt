package voice.features.bookmark

import voice.core.data.Book
import voice.core.data.BookContent
import voice.core.data.BookId
import voice.core.data.BookMetadata
import voice.core.data.Bookmark
import voice.core.data.Chapter
import voice.core.data.ChapterId
import voice.core.data.ListeningEvent
import voice.core.data.MarkData
import voice.core.ui.formatTime
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.uuid.Uuid

internal val testZone: ZoneId = ZoneId.of("Europe/Berlin")

internal fun today(
  hour: Int,
  minute: Int = 0,
): Instant = ZonedDateTime.of(2026, 10, 7, hour, minute, 0, 0, testZone).toInstant()

internal fun yesterday(
  hour: Int,
  minute: Int = 0,
): Instant = ZonedDateTime.of(2026, 10, 6, hour, minute, 0, 0, testZone).toInstant()

internal fun testChapter(
  duration: Duration = 10.minutes,
  name: String? = null,
  marks: List<Pair<Duration, String>> = emptyList(),
): Chapter = Chapter(
  id = ChapterId("http://${Uuid.random()}"),
  name = name,
  duration = duration.inWholeMilliseconds,
  fileLastModified = Instant.EPOCH,
  fileSize = 0,
  markData = marks.map { (start, markName) -> MarkData(startMs = start.inWholeMilliseconds, name = markName) },
)

internal fun testBook(
  chapters: List<Chapter>,
  currentChapter: ChapterId = chapters.first().id,
  positionInChapter: Duration = Duration.ZERO,
): Book = Book(
  content = BookContent(
    positionInChapter = positionInChapter.inWholeMilliseconds,
    playbackSpeed = 1F,
    addedAt = Instant.EPOCH,
    chapters = chapters.map { it.id },
    currentChapter = currentChapter,
    isActive = true,
    lastPlayedAt = Instant.EPOCH,
    skipSilence = false,
    id = BookId("echoes"),
    gain = 0F,
    metadata = BookMetadata(
      name = "Echoes of Tomorrow",
      author = "Daniel Hartwell",
      album = null,
      title = null,
      genre = null,
      narrator = null,
      series = null,
      part = null,
      cover = null,
      fileLastModified = null,
    ),
    customName = null,
    customCover = null,
  ),
  chapters = chapters,
)

internal fun testBookmark(
  chapter: Chapter,
  time: Duration,
  addedAt: Instant = today(9),
  title: String? = null,
  kind: Bookmark.Kind = Bookmark.Kind.Note,
): Bookmark = Bookmark(
  bookId = BookId("echoes"),
  chapterId = chapter.id,
  title = title,
  time = time.inWholeMilliseconds,
  addedAt = addedAt,
  id = Bookmark.Id(Uuid.random()),
  kind = kind,
)

private var nextEventId = 1L

internal fun testEvent(
  type: ListeningEvent.Type,
  at: Instant,
  chapter: Chapter,
  time: Duration,
  source: ListeningEvent.Source = ListeningEvent.Source.App,
  to: Pair<Chapter, Duration>? = null,
): ListeningEvent = ListeningEvent(
  bookId = BookId("echoes"),
  type = type,
  source = source,
  atMillis = at.toEpochMilli(),
  chapterId = chapter.id,
  time = time.inWholeMilliseconds,
  toChapterId = to?.first?.id,
  toTime = to?.second?.inWholeMilliseconds,
  id = nextEventId++,
)

/**
 * A time as the bookmark and history rows show it. A chapter without marks has a single mark
 * that ends a millisecond before the chapter does.
 */
internal fun chapterTime(
  time: Duration,
  chapterDuration: Duration = 10.minutes,
): String = formatTime(time.inWholeMilliseconds, chapterDuration.inWholeMilliseconds - 1)
