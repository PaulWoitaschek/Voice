package voice.features.bookmark.history

import voice.core.data.Bookmark
import voice.core.data.ChapterId
import voice.core.data.ListeningEvent
import voice.core.data.ListeningEvent.Source
import voice.core.data.ListeningEvent.Type
import voice.core.data.at
import voice.features.bookmark.BookIndex
import voice.features.bookmark.sessionLabel
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.absoluteValue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/** A pause at least this long starts a new listening session. */
private val SESSION_GAP = 15.minutes

/** Skips, seeks and chapter changes this close to each other merge into one row. */
private val MERGE_WINDOW = 60.seconds

/** Seeks shorter than this are just adjusting, not worth a row. */
private val TINY_SEEK = 5.seconds

/** Seeks at least this long can be an accident, the same as the back pill on the player. */
private val BIG_JUMP = 30.seconds

private val STARTED_BY_RECENT = 24.hours
private val STARTED_BY_MIN_PLAYED = 1.minutes
private val JUMPED_RECENT = 10.minutes

private val mergeableTypes = setOf(Type.Seek, Type.SkipBack, Type.SkipForward, Type.ChapterChange)
private val jumpTypes = setOf(Type.Seek, Type.SkipBack, Type.SkipForward, Type.ChapterChange, Type.BookmarkJump)

/** Sources that mean a person did something on purpose. */
private val touchSources = setOf(
  Source.App,
  Source.Widget,
  Source.Notification,
  Source.Headset,
  Source.Bluetooth,
  Source.Car,
  Source.Watch,
)

/** Sources that can start playback without the listener meaning to. */
private val startedBySources = setOf(Source.Car, Source.Bluetooth, Source.Headset, Source.Watch, Source.OtherApp)

internal fun historyViewState(
  events: List<ListeningEvent>,
  index: BookIndex,
  bookmarks: List<Bookmark>,
  enabled: Boolean,
  playing: Boolean,
  selectedFilter: HistoryFilter?,
  selectedSource: Source?,
  dismissedSuggestion: Long?,
  now: Instant,
  zone: ZoneId,
): HistoryViewState {
  val ascending = events.sortedWith(compareBy({ it.atMillis }, { it.id }))
  val previousValues = previousSettingValues(ascending)
  val bookmarkIds = bookmarks.mapTo(mutableSetOf()) { it.id }
  val rawSessions = splitSessions(ascending)
  val sessions = rawSessions.mapIndexed { sessionIndex, session ->
    val ongoing = sessionIndex == rawSessions.lastIndex && playing
    buildSession(session, ongoing, index, previousValues, bookmarkIds, now, zone)
  }
  val allEntries = sessions.flatMap { it.entries }
  val filters = HistoryFilter.entries.filter { filter -> allEntries.any { filter in it.filters() } }
  val sources = allEntries.map { it.source }.distinct().sorted()
  val filter = selectedFilter?.takeIf { it in filters }
  val source = selectedSource?.takeIf { it in sources }
  val visibleSessions = sessions
    .map { session ->
      session.copy(
        entries = session.entries.filter { entry ->
          (filter == null || filter in entry.filters()) && (source == null || entry.source == source)
        },
      )
    }
    .filter { it.entries.isNotEmpty() }
    .reversed()
  return HistoryViewState(
    enabled = enabled,
    suggestion = suggestion(rawSessions, index, playing, now, zone)?.takeIf { it.key != dismissedSuggestion },
    filters = filters,
    selectedFilter = filter,
    sources = sources,
    selectedSource = source,
    sessions = visibleSessions,
    hasEvents = events.isNotEmpty(),
  )
}

internal fun HistoryEntry.filters(): Set<HistoryFilter> {
  val filters = mutableSetOf<HistoryFilter>()
  when (type) {
    Type.Play, Type.Pause -> filters += HistoryFilter.PlayPause
    Type.Seek, Type.SkipBack, Type.SkipForward, Type.ChapterChange, Type.JumpBack -> filters += HistoryFilter.Jumps
    Type.BookmarkJump -> {
      filters += HistoryFilter.Jumps
      filters += HistoryFilter.Bookmarks
    }
    Type.SpeedChanged, Type.SkipSilenceChanged, Type.VolumeBoostChanged -> filters += HistoryFilter.Settings
    Type.SleepTimerSet, Type.SleepTimerEnded, Type.SleepTimerExtended -> filters += HistoryFilter.Sleep
    Type.BookmarkAdded, Type.BookmarkDeleted -> filters += HistoryFilter.Bookmarks
  }
  if (lastTouch) filters += HistoryFilter.Sleep
  return filters
}

/**
 * Splits the events, oldest first, into listening sessions. Only a pause starts a new session,
 * listening for hours without touching anything stays one session.
 */
internal fun splitSessions(events: List<ListeningEvent>): List<List<ListeningEvent>> {
  val sessions = mutableListOf<MutableList<ListeningEvent>>()
  var playing = false
  var previous: ListeningEvent? = null
  events.forEach { event ->
    val gap = previous?.let { (event.atMillis - it.atMillis).milliseconds }
    if (gap == null || (!playing && gap >= SESSION_GAP)) {
      sessions += mutableListOf<ListeningEvent>()
    }
    sessions.last() += event
    playing = when (event.type) {
      Type.Play, Type.SleepTimerExtended -> true
      Type.Pause, Type.SleepTimerEnded -> false
      else -> playing
    }
    previous = event
  }
  return sessions
}

private fun buildSession(
  events: List<ListeningEvent>,
  ongoing: Boolean,
  index: BookIndex,
  previousValues: Map<Long, String?>,
  bookmarkIds: Set<Bookmark.Id>,
  now: Instant,
  zone: ZoneId,
): HistorySession {
  val first = events.first()
  val endMillis = if (ongoing) now.toEpochMilli() else events.last().atMillis
  val positions = events.flatMap { event ->
    listOfNotNull(
      index.locate(event.chapterId, event.time)?.progress,
      event.toChapterId?.let { chapterId -> index.locate(chapterId, event.toTime ?: 0L)?.progress },
    )
  } + listOfNotNull(index.current?.progress?.takeIf { ongoing })
  return HistorySession(
    key = first.id,
    label = sessionLabel(first.at, now, zone),
    start = first.at.localTime(zone),
    end = Instant.ofEpochMilli(endMillis).localTime(zone),
    listened = listenedMillis(events, endMillis).milliseconds,
    barStart = positions.minOrNull() ?: 0F,
    barEnd = positions.maxOrNull() ?: 0F,
    entries = buildEntries(events, index, previousValues, bookmarkIds, zone).reversed(),
  )
}

private fun listenedMillis(
  events: List<ListeningEvent>,
  endMillis: Long,
): Long {
  var listened = 0L
  var playStart: Long? = null
  events.forEach { event ->
    when (event.type) {
      Type.Play, Type.SleepTimerExtended -> if (playStart == null) playStart = event.atMillis
      Type.Pause, Type.SleepTimerEnded -> {
        playStart?.let { listened += event.atMillis - it }
        playStart = null
      }
      else -> {}
    }
  }
  playStart?.let { listened += endMillis - it }
  return listened.coerceAtLeast(0L)
}

/** Events that make up one row: repeats merged, and a pause with the play that ended it. */
private class Row(val first: ListeningEvent) {
  var last: ListeningEvent = first
  var count: Int = 1
  var resumedAt: Long? = null
}

private fun buildEntries(
  events: List<ListeningEvent>,
  index: BookIndex,
  previousValues: Map<Long, String?>,
  bookmarkIds: Set<Bookmark.Id>,
  zone: ZoneId,
): List<HistoryEntry> {
  val rows = mutableListOf<Row>()
  events.forEach { event ->
    if (event.isTinySeek()) return@forEach
    val previous = rows.lastOrNull()
    when {
      previous != null &&
        event.type in mergeableTypes &&
        previous.first.type == event.type &&
        (event.atMillis - previous.last.atMillis).milliseconds <= MERGE_WINDOW -> {
        previous.last = event
        previous.count++
      }
      previous != null &&
        event.type == Type.Play &&
        previous.first.type == Type.Pause &&
        previous.resumedAt == null &&
        (event.source == Source.App || event.source == Source.AudioFocus || event.source == previous.first.source) -> {
        previous.resumedAt = event.atMillis
      }
      else -> rows += Row(event)
    }
  }
  val lastTouches = lastTouches(rows)
  return rows.mapIndexed { rowIndex, row ->
    val first = row.first
    val lastTouch = rowIndex in lastTouches
    HistoryEntry(
      key = first.id,
      at = first.at.localTime(zone),
      type = first.type,
      source = first.source,
      count = row.count,
      where = index.historyLocation(first.chapterId, first.time),
      to = row.last.toChapterId?.let { index.historyLocation(it, row.last.toTime ?: 0L) },
      detail = detail(row, previousValues),
      lastTouch = lastTouch,
      action = if (lastTouch) {
        val (chapterId, time) = row.last.landedAt()
        HistoryAction.GoThere(chapterId, time)
      } else {
        action(row, index, previousValues, bookmarkIds)
      },
    )
  }
}

/** For every time the sleep timer ended, the last row someone touched before that. */
private fun lastTouches(rows: List<Row>): Set<Int> {
  val touches = mutableSetOf<Int>()
  rows.forEachIndexed { rowIndex, row ->
    if (row.first.type == Type.SleepTimerEnded) {
      val touch = (rowIndex - 1 downTo 0).firstOrNull { rows[it].first.source in touchSources }
      if (touch != null) touches += touch
    }
  }
  return touches
}

private fun ListeningEvent.landedAt(): Pair<ChapterId, Long> {
  val toChapterId = toChapterId
  return if (toChapterId != null) toChapterId to (toTime ?: 0L) else chapterId to time
}

private fun ListeningEvent.isTinySeek(): Boolean {
  val toTime = toTime ?: return false
  return type == Type.Seek && toChapterId == chapterId && (toTime - time).absoluteValue < TINY_SEEK.inWholeMilliseconds
}

private fun ListeningEvent.isBigJump(): Boolean {
  return when (type) {
    Type.ChapterChange, Type.BookmarkJump -> true
    Type.Seek -> {
      val toTime = toTime
      toChapterId != chapterId || toTime == null || (toTime - time).absoluteValue >= BIG_JUMP.inWholeMilliseconds
    }
    else -> false
  }
}

private fun detail(
  row: Row,
  previousValues: Map<Long, String?>,
): HistoryDetail? {
  val event = row.first
  val previous = previousValues[event.id]
  return when (event.type) {
    Type.Pause -> row.resumedAt?.let { HistoryDetail.PausedFor((it - event.atMillis).milliseconds) }
    Type.SkipBack, Type.SkipForward -> {
      val seconds = event.value?.toIntOrNull() ?: return null
      HistoryDetail.Skipped(seconds * row.count)
    }
    Type.SpeedChanged -> event.value?.toFloatOrNull()?.let { HistoryDetail.Speed(previous?.toFloatOrNull(), it) }
    Type.VolumeBoostChanged -> event.value?.toFloatOrNull()?.let {
      HistoryDetail.VolumeBoost(previous?.toFloatOrNull(), it)
    }
    Type.SkipSilenceChanged -> event.value?.toBooleanStrictOrNull()?.let { HistoryDetail.SkipSilence(it) }
    Type.SleepTimerSet, Type.SleepTimerExtended -> HistoryDetail.SleepTimer(event.value?.toIntOrNull())
    Type.BookmarkAdded, Type.BookmarkDeleted -> HistoryDetail.BookmarkInfo(
      kind = event.bookmarkKind ?: Bookmark.Kind.Note,
      setBySleepTimer = event.bookmarkSetBySleepTimer ?: false,
      note = event.value,
    )
    else -> null
  }
}

private fun action(
  row: Row,
  index: BookIndex,
  previousValues: Map<Long, String?>,
  bookmarkIds: Set<Bookmark.Id>,
): HistoryAction? {
  val event = row.first
  return when (event.type) {
    Type.Seek, Type.SkipBack, Type.SkipForward, Type.ChapterChange, Type.BookmarkJump -> index.jumpBack(event)
    Type.Play -> if (event.source in startedBySources) {
      index.jumpBack(event)
    } else {
      HistoryAction.Pin(event.chapterId, event.time)
    }
    Type.JumpBack -> {
      val (chapterId, time) = event.landedAt()
      HistoryAction.Pin(chapterId, time)
    }
    Type.Pause, Type.SleepTimerEnded, Type.SleepTimerSet, Type.SleepTimerExtended -> {
      HistoryAction.Pin(event.chapterId, event.time)
    }
    Type.SpeedChanged, Type.VolumeBoostChanged -> {
      previousValues[event.id]?.let { HistoryAction.ChangeBack(event.type, it) }
    }
    Type.SkipSilenceChanged -> {
      event.value?.toBooleanStrictOrNull()?.let { HistoryAction.ChangeBack(event.type, (!it).toString()) }
    }
    Type.BookmarkDeleted -> {
      val id = event.bookmarkId ?: return null
      if (id in bookmarkIds) return null
      HistoryAction.Restore(
        Bookmark(
          bookId = event.bookId,
          chapterId = event.chapterId,
          title = event.value,
          time = event.time,
          addedAt = event.at,
          setBySleepTimer = event.bookmarkSetBySleepTimer ?: false,
          id = id,
          kind = event.bookmarkKind ?: Bookmark.Kind.Note,
        ),
      )
    }
    Type.BookmarkAdded -> null
  }
}

/**
 * The value a setting had before each change, by event id. Null when the change is the first
 * one recorded.
 */
private fun previousSettingValues(ascending: List<ListeningEvent>): Map<Long, String?> {
  val latest = mutableMapOf<Type, String?>()
  val previous = mutableMapOf<Long, String?>()
  ascending.forEach { event ->
    if (event.type == Type.SpeedChanged || event.type == Type.VolumeBoostChanged) {
      previous[event.id] = latest[event.type]
      latest[event.type] = event.value
    }
  }
  return previous
}

/**
 * The newest thing that looks like an accident: playback started by a car, headset or watch,
 * or a big jump in the last minutes that wasn't undone.
 */
private fun suggestion(
  rawSessions: List<List<ListeningEvent>>,
  index: BookIndex,
  playing: Boolean,
  now: Instant,
  zone: ZoneId,
): HistorySuggestion? {
  val latest = rawSessions.lastOrNull() ?: return null
  val startedBy = startedBySuggestion(latest, index, playing, now, zone)
  val jumped = jumpedSuggestion(latest, index, now, zone)
  return listOfNotNull(startedBy, jumped).maxByOrNull { it.key }
}

private fun startedBySuggestion(
  session: List<ListeningEvent>,
  index: BookIndex,
  playing: Boolean,
  now: Instant,
  zone: ZoneId,
): HistorySuggestion.StartedBy? {
  val play = session.firstOrNull { it.type == Type.Play } ?: return null
  if (play.source !in startedBySources) return null
  if ((now.toEpochMilli() - play.atMillis).milliseconds > STARTED_BY_RECENT) return null
  val after = session.dropWhile { it !== play }.drop(1)
  // touching the app afterwards, or going back already, means it was fine
  if (after.any { it.source == Source.App || it.type == Type.JumpBack }) return null
  val endMillis = if (playing) now.toEpochMilli() else session.last().atMillis
  val played = listenedMillis(listOf(play) + after, endMillis).milliseconds
  if (played < STARTED_BY_MIN_PLAYED) return null
  val back = index.jumpBack(play) ?: return null
  return HistorySuggestion.StartedBy(
    key = play.id,
    back = back,
    source = play.source,
    at = play.at.localTime(zone),
    played = played,
  )
}

private fun jumpedSuggestion(
  session: List<ListeningEvent>,
  index: BookIndex,
  now: Instant,
  zone: ZoneId,
): HistorySuggestion.Jumped? {
  val jump = session.lastOrNull { it.type in jumpTypes && it.isBigJump() } ?: return null
  if ((now.toEpochMilli() - jump.atMillis).milliseconds > JUMPED_RECENT) return null
  if (session.any { it.atMillis >= jump.atMillis && it.type == Type.JumpBack }) return null
  val back = index.jumpBack(jump) ?: return null
  return HistorySuggestion.Jumped(
    key = jump.id,
    back = back,
    at = jump.at.localTime(zone),
  )
}

private fun BookIndex.jumpBack(event: ListeningEvent): HistoryAction.JumpBack? {
  val location = historyLocation(event.chapterId, event.time) ?: return null
  return HistoryAction.JumpBack(event.chapterId, event.time, location)
}

internal fun BookIndex.historyLocation(
  chapterId: ChapterId,
  time: Long,
): HistoryLocation? {
  val location = locate(chapterId, time) ?: return null
  return HistoryLocation(
    chapterNumber = location.chapterNumber.takeIf { chapterCount > 1 },
    time = location.time,
  )
}

private fun Instant.localTime(zone: ZoneId): LocalTime = atZone(zone).toLocalTime().withSecond(0).withNano(0)
