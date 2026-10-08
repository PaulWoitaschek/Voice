package voice.features.widget

import android.graphics.Bitmap
import voice.core.data.Book
import voice.core.data.BookId
import voice.core.data.ThemeColorScheme
import voice.core.sleeptimer.SleepTimerState
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

/** The colors of a widget: from the [seed] of a cover, or else from the app's [themeColorScheme]. */
internal data class WidgetTheme(
  val seed: Int?,
  val themeColorScheme: ThemeColorScheme,
)

/** A cover ready to be drawn. The [key] identifies the cover, so its shaped versions can be cached. */
internal data class WidgetCover(
  val key: String,
  val bitmap: Bitmap,
)

internal data class NowPlayingModel(
  val book: NowPlayingBook?,
  val libraryEmpty: Boolean,
  val themeColorScheme: ThemeColorScheme,
)

internal sealed interface NowPlayingState {

  val theme: WidgetTheme

  data class Empty(
    val libraryEmpty: Boolean,
    override val theme: WidgetTheme,
  ) : NowPlayingState

  data class Current(
    val book: NowPlayingBook,
    val cover: WidgetCover,
    override val theme: WidgetTheme,
  ) : NowPlayingState
}

internal data class NowPlayingBook(
  val title: String,
  val cover: String?,
  val chapterName: String?,
  val chapterNumber: Int,
  val chapterCount: Int,
  val progress: Float,
  val remaining: Duration,
  val duration: Duration,
  val speed: Float,
  val playing: Boolean,
  val finished: Boolean,
  val sleepTimerEnd: SleepTimerEnd?,
)

/** When a running sleep timer stops playback. [at] is null when that can't be known. */
internal data class SleepTimerEnd(
  val at: Instant?,
  val endOfChapter: Boolean,
)

internal data class ShelfModel(
  val books: List<ShelfBook>,
  val themeColorScheme: ThemeColorScheme,
)

internal data class ShelfState(
  val books: List<ShelfItem>,
  val theme: WidgetTheme,
)

internal data class ShelfItem(
  val book: ShelfBook,
  val cover: WidgetCover,
)

internal data class ShelfBook(
  val id: BookId,
  val title: String,
  val cover: String?,
  val progress: Float,
  val playing: Boolean,
)

internal data class SleepTimerModel(
  val sleepTimer: SleepTimerWidgetModel,
  val cover: String?,
  val themeColorScheme: ThemeColorScheme,
)

internal data class SleepTimerWidgetState(
  val sleepTimer: SleepTimerWidgetModel,
  val theme: WidgetTheme,
)

internal sealed interface SleepTimerWidgetModel {

  data class Ready(val duration: Duration) : SleepTimerWidgetModel

  data class Running(val end: SleepTimerEnd) : SleepTimerWidgetModel
}

internal const val SHELF_MAX_BOOKS = 5

/**
 * The position only counts in whole minutes, so a playing book changes the widget once a minute and
 * not with every position update.
 */
internal fun nowPlayingBook(
  book: Book,
  playing: Boolean,
  sleepTimer: SleepTimerState,
  now: Instant,
): NowPlayingBook {
  val chapterCount = book.chapters.sumOf { it.chapterMarks.count() }
  val chapterNumber = book.chapters.take(book.content.currentChapterIndex).sumOf { it.chapterMarks.count() } +
    book.currentChapter.chapterMarks.indexOf(book.currentMark) + 1
  val speed = book.content.playbackSpeed
  val position = book.position / MINUTE_MS * MINUTE_MS
  return NowPlayingBook(
    title = book.content.name,
    cover = book.content.coverUrl,
    chapterName = book.currentMark.name?.takeIf { chapterCount > 1 && it.isNotBlank() },
    chapterNumber = chapterNumber.coerceAtLeast(1),
    chapterCount = chapterCount,
    progress = progress(position, book.duration),
    remaining = ((book.duration - position).coerceAtLeast(0).milliseconds / speed.atSpeed()).roundUpToMinutes(),
    duration = book.duration.milliseconds,
    speed = speed,
    playing = playing,
    finished = book.finished(),
    sleepTimerEnd = sleepTimerEnd(sleepTimer, book, now),
  )
}

/** Rounded to the minute, as it's shown as a clock time. */
internal fun sleepTimerEnd(
  state: SleepTimerState,
  book: Book?,
  now: Instant,
): SleepTimerEnd? {
  return when (state) {
    SleepTimerState.Disabled -> null
    is SleepTimerState.Enabled.WithDuration -> SleepTimerEnd(
      at = (now + state.leftDuration.toJavaDuration()).roundToMinute(),
      endOfChapter = false,
    )
    SleepTimerState.Enabled.WithEndOfChapter -> SleepTimerEnd(
      at = book?.let {
        val leftInChapter = (it.currentMark.endMs - it.content.positionInChapter).coerceAtLeast(0).milliseconds
        (now + (leftInChapter / it.content.playbackSpeed.atSpeed()).toJavaDuration()).roundToMinute()
      },
      endOfChapter = true,
    )
  }
}

/**
 * The books someone is in the middle of, the current one first, then the most recently played. The
 * current book is on it even before it was started, as it's what plays next.
 */
internal fun shelfBooks(
  books: List<Book>,
  currentBookId: BookId?,
  playing: Boolean,
): List<ShelfBook> {
  return books
    .filter { !it.finished() && (it.position > 0 || it.id == currentBookId) }
    .sortedWith(compareByDescending<Book> { it.id == currentBookId }.thenByDescending { it.content.lastPlayedAt })
    .take(SHELF_MAX_BOOKS)
    .map { book ->
      ShelfBook(
        id = book.id,
        title = book.content.name,
        cover = book.content.coverUrl,
        progress = (progress(book.position, book.duration) * 100).roundToInt() / 100F,
        playing = playing && book.id == currentBookId,
      )
    }
}

internal fun sleepTimerWidgetModel(
  state: SleepTimerState,
  defaultDuration: Duration,
  book: Book?,
  now: Instant,
): SleepTimerWidgetModel {
  val end = sleepTimerEnd(state, book, now)
  return if (end == null) {
    SleepTimerWidgetModel.Ready(defaultDuration)
  } else {
    SleepTimerWidgetModel.Running(end)
  }
}

private fun progress(
  position: Long,
  duration: Long,
): Float {
  return if (duration > 0) {
    (position.toFloat() / duration).coerceIn(0F, 1F)
  } else {
    0F
  }
}

// the same rule the library uses to show a book as completed
private fun Book.finished(): Boolean = position > 0 && position >= duration - 5.seconds.inWholeMilliseconds

private fun Float.atSpeed(): Double = coerceAtLeast(0.1F).toDouble()

private fun Duration.roundUpToMinutes(): Duration = ceil(this / 1.minutes).minutes

private fun Instant.roundToMinute(): Instant = plusSeconds(30).truncatedTo(ChronoUnit.MINUTES)

private const val MINUTE_MS = 60_000L
