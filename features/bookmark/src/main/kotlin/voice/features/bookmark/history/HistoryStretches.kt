package voice.features.bookmark.history

import voice.core.data.ChapterId
import voice.core.data.ListeningEvent
import voice.core.data.ListeningEvent.Source
import voice.core.data.ListeningEvent.Type
import voice.core.data.isBigJump
import voice.core.data.isUndoableJump
import voice.features.bookmark.BookIndex
import voice.features.bookmark.sessionLabel
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/** A pause at least this long starts a new stretch. */
private val STRETCH_GAP = 15.minutes

/** Shorter stretches, like the moments between skipping through chapters, aren't worth a row. */
private val MIN_STRETCH = 10.seconds

/** Stops this close to each other are the same stop, recorded twice. */
private const val SAME_STOP = 5_000L

private val playTypes = setOf(Type.Play, Type.SleepTimerExtended)
private val stopTypes = setOf(Type.Pause, Type.SleepTimerEnded)

/**
 * The history without what changes while listening.
 *
 * Building it goes through all events, too much for every position update, so it's only built when
 * the events change. [viewState] adds the rest.
 */
internal data class HistoryStretches(
  val index: BookIndex,
  private val zone: ZoneId,
  private val days: List<HistoryDay>,
  private val ongoing: OngoingStretch?,
) {

  /** Whether the newest stretch is still playing, so it grows with time and the position. */
  val isOngoing: Boolean get() = ongoing != null

  fun viewState(
    now: Instant,
    current: HistorySpot?,
  ): HistoryViewState {
    if (ongoing == null) return HistoryViewState(days)
    return HistoryViewState(
      days.map { day ->
        val position = day.rows.indexOfFirst { it is HistoryStretch && it.key == ongoing.key }
        if (position == -1) return@map day
        val playing = day.rows[position] as HistoryStretch
        val updated = playing.copy(
          end = now.localTime(zone),
          listened = ongoing.listened(now.toEpochMilli()),
          to = current?.let { index.historyLocation(it.chapterId, it.time) } ?: playing.to,
        )
        day.copy(rows = day.rows.toMutableList().apply { set(position, updated) })
      },
    )
  }
}

internal data class OngoingStretch(
  val key: Long,
  val closedMillis: Long,
  val openSince: Long,
) {

  fun listened(nowMillis: Long): Duration = (closedMillis + nowMillis - openSince).coerceAtLeast(0L).milliseconds
}

internal fun historyStretches(
  events: List<ListeningEvent>,
  index: BookIndex,
  playing: Boolean,
  now: Instant,
  zone: ZoneId,
): HistoryStretches {
  val (raw, rawJumps) = splitHistory(events.sortedWith(compareBy({ it.atMillis }, { it.id })))
  val ongoing = raw.lastOrNull()?.takeIf { playing && it.openSince != null }
  raw.filter { it !== ongoing }.forEach { it.closeLostPause() }
  val stretches = raw
    .filter {
      it === ongoing || it.endUnknown || it.cause == StretchCause.SleepTimerStarted ||
        it.closedMillis >= MIN_STRETCH.inWholeMilliseconds
    }
  val jumps = rawJumps
    .filterNot { it.isUndone() }
    .mapNotNull { jump -> jump.toJump(index, zone)?.let { jump to it } }
  val jumpsWithRows = jumps.mapTo(mutableSetOf()) { it.first }
  val stretchRows = stretches.mapNotNull { stretch ->
    stretch.toStretch(index, zone, isOngoing = stretch === ongoing, jumpsWithRows)?.let { stretch.startMillis to it }
  }
  val jumpRows = jumps.map { (jump, row) -> jump.atMillis to row }
  // a jump while playing starts a stretch at the same moment, and that stretch came after it
  val rows = (stretchRows + jumpRows).sortedWith(
    compareByDescending<Pair<Long, HistoryRow>> { it.first }.thenBy { it.second is HistoryJump },
  )
  val days = mutableListOf<HistoryDay>()
  rows.forEach { (atMillis, row) ->
    val label = sessionLabel(Instant.ofEpochMilli(atMillis), now, zone)
    val last = days.lastOrNull()
    if (last != null && last.label == label) {
      days[days.lastIndex] = last.copy(rows = last.rows + row)
    } else {
      days += HistoryDay(label, listOf(row))
    }
  }
  return HistoryStretches(
    index = index,
    zone = zone,
    days = days,
    ongoing = ongoing?.let { OngoingStretch(key = it.key, closedMillis = it.closedMillis, openSince = it.openSince!!) },
  )
}

/** One stretch while building them, oldest event first. */
private class RawStretch(
  val key: Long,
  val startMillis: Long,
  val from: HistorySpot,
  val cause: StretchCause,
  val startedBy: RawJump? = null,
) {
  var endMillis: Long = startMillis
  var to: HistorySpot? = from
  var endedBySleepTimer: Boolean = false
  var closedMillis: Long = 0L

  /** Set while playing. */
  var openSince: Long? = null
  var lastEventMillis: Long = startMillis
  var lastSpot: HistorySpot = from

  /** Where the listener last did something, so where they were surely still awake. */
  var lastTouch: HistorySpot = from

  /** Playback was lost without a pause and nothing came after it, so where and when it ended is unknown. */
  var endUnknown: Boolean = false

  fun stop(
    atMillis: Long,
    spot: HistorySpot,
  ) {
    openSince?.let { closedMillis += atMillis - it }
    openSince = null
    endMillis = atMillis
    to = spot
  }

  /** Playback that never got a pause, because the app was killed, ends with its last event, if there was one. */
  fun closeLostPause() {
    val since = openSince ?: return
    endUnknown = lastEventMillis == since
    stop(lastEventMillis, lastSpot)
  }
}

/** A big jump while building them, or several without listening in between. */
private class RawJump(
  first: ListeningEvent,
  listenedMillis: Long,
) {
  val key: Long = first.id
  val atMillis: Long = first.atMillis
  val from: HistorySpot = first.spot()
  val type: Type = first.type
  val source: Source = first.source
  var to: HistorySpot? = first.target()
  private var lastMillis = first.atMillis
  private var merged = false
  private var listenedAt = listenedMillis

  /** Whether [event] goes on searching, or undoes this jump, before the listener heard much where it landed. */
  fun continuesWith(
    event: ListeningEvent,
    listenedMillis: Long,
  ): Boolean {
    return listenedMillis - listenedAt < MIN_STRETCH.inWholeMilliseconds &&
      event.atMillis - lastMillis < STRETCH_GAP.inWholeMilliseconds
  }

  fun add(
    event: ListeningEvent,
    listenedMillis: Long,
  ) {
    to = event.target()
    merged = true
    lastMillis = event.atMillis
    listenedAt = listenedMillis
  }

  /** Jumps that ended up back where they left, like one undone with the pill right away. */
  fun isUndone(): Boolean {
    val to = to ?: return false
    return merged && !isBigJump(from.chapterId, from.time, to.chapterId, to.time)
  }
}

/**
 * Splits the events, oldest first, into stretches of listening and the big jumps between them. A
 * pause of [STRETCH_GAP] or more starts a new stretch, and so does a big jump while playing, so the
 * place before an accidental jump is where the stretch before it ends.
 */
private fun splitHistory(events: List<ListeningEvent>): Pair<List<RawStretch>, List<RawJump>> {
  val stretches = mutableListOf<RawStretch>()
  val jumps = mutableListOf<RawJump>()
  var current: RawStretch? = null
  // a big jump while paused starts the next stretch, once playback goes on
  var pendingJump: ListeningEvent? = null
  // how long playback ran, to tell whether the listener heard anything between two jumps
  var listenedMillis = 0L
  var previousMillis = 0L

  fun open(
    event: ListeningEvent,
    from: HistorySpot,
    cause: StretchCause,
    startedBy: RawJump? = null,
  ) {
    val stretch = RawStretch(key = event.id, startMillis = event.atMillis, from = from, cause = cause, startedBy = startedBy)
    stretch.openSince = event.atMillis
    stretches += stretch
    current = stretch
  }

  events.forEach { event ->
    if (current?.openSince != null) listenedMillis += event.atMillis - previousMillis
    previousMillis = event.atMillis
    val stretch = current
    when {
      event.type in playTypes -> {
        if (stretch != null) {
          val since = stretch.openSince
          val gap = event.atMillis - (if (since != null) stretch.lastEventMillis else stretch.endMillis)
          if (gap >= STRETCH_GAP.inWholeMilliseconds) {
            // a play while playing means the pause before it was lost
            stretch.closeLostPause()
            current = null
          } else {
            if (since != null) stretch.closedMillis += stretch.lastEventMillis - since
            stretch.openSince = event.atMillis
            stretch.endedBySleepTimer = false
            stretch.lastTouch = event.spot()
          }
        }
        if (current == null) {
          val jump = pendingJump?.takeIf { event.atMillis - it.atMillis < STRETCH_GAP.inWholeMilliseconds }
          if (jump != null) {
            open(event, event.spot(), StretchCause.Jumped(jump.type, jump.source, hasRow = false), startedBy = jumps.last())
          } else {
            open(event, event.spot(), StretchCause.Played(event.source))
          }
          pendingJump = null
        }
      }
      event.type in stopTypes -> {
        if (stretch != null && stretch.openSince != null) {
          stretch.stop(event.atMillis, event.spot())
          stretch.endedBySleepTimer = event.type == Type.SleepTimerEnded
        } else if (stretch != null && event.type == Type.SleepTimerEnded && event.atMillis - stretch.endMillis <= SAME_STOP) {
          // the end of the book and the end of chapter timer can stop playback at the same moment
          stretch.endedBySleepTimer = true
        }
      }
      event.isUndoableJump() || event.type == Type.JumpBack -> {
        val last = jumps.lastOrNull()
        val jump = if (last != null && last.continuesWith(event, listenedMillis)) {
          last.also { it.add(event, listenedMillis) }
        } else {
          RawJump(event, listenedMillis).also { jumps += it }
        }
        if (stretch != null && stretch.openSince != null) {
          stretch.stop(event.atMillis, event.spot())
          open(event, event.landedAt(), StretchCause.Jumped(event.type, event.source, hasRow = false), startedBy = jump)
        } else {
          current = null
          pendingJump = event
        }
      }
      event.type == Type.SleepTimerSet && event.source == Source.SleepTimer && stretch?.openSince == null -> {
        // a night that only a sleep timer bookmark remembered, so only its start is known
        current = null
        stretches += RawStretch(key = event.id, startMillis = event.atMillis, from = event.spot(), cause = StretchCause.SleepTimerStarted)
          .also { it.to = null }
      }
      else -> {}
    }
    current?.let {
      it.lastEventMillis = event.atMillis
      it.lastSpot = event.landedAt()
    }
  }
  return stretches to jumps
}

private fun RawStretch.toStretch(
  index: BookIndex,
  zone: ZoneId,
  isOngoing: Boolean,
  jumpsWithRows: Set<RawJump>,
): HistoryStretch? {
  val from = index.historyLocation(from.chapterId, from.time) ?: return null
  val to = to?.let { index.historyLocation(it.chapterId, it.time) }
  val unknownEnd = endUnknown || cause == StretchCause.SleepTimerStarted
  // after falling asleep, playback is at the end already, so this goes to where the listener was still awake
  val target = if (endedBySleepTimer || unknownEnd) lastTouch else this.to
  val targetLocation = target?.let { index.historyLocation(it.chapterId, it.time) }
  return HistoryStretch(
    key = key,
    start = Instant.ofEpochMilli(startMillis).localTime(zone),
    end = if (unknownEnd) null else Instant.ofEpochMilli(endMillis).localTime(zone),
    listened = if (unknownEnd) null else closedMillis.milliseconds,
    from = from,
    to = to.takeUnless { unknownEnd },
    cause = when {
      endedBySleepTimer -> StretchCause.SleepTimerEnded
      // several jumps can be one row, and the stretch shows the same one
      startedBy != null && startedBy in jumpsWithRows -> StretchCause.Jumped(startedBy.type, startedBy.source, hasRow = true)
      else -> cause
    },
    goTo = if (target == null || targetLocation == null || isOngoing) null else HistoryTarget(target, targetLocation),
  )
}

private fun RawJump.toJump(
  index: BookIndex,
  zone: ZoneId,
): HistoryJump? {
  val from = index.historyLocation(from.chapterId, from.time) ?: return null
  return HistoryJump(
    key = key,
    at = Instant.ofEpochMilli(atMillis).localTime(zone),
    from = from,
    to = to?.let { index.historyLocation(it.chapterId, it.time) },
    type = type,
    source = source,
    back = HistoryTarget(this.from, from),
  )
}

private fun ListeningEvent.spot(): HistorySpot = HistorySpot(chapterId, time)

private fun ListeningEvent.target(): HistorySpot? = toChapterId?.let { HistorySpot(it, toTime ?: 0L) }

private fun ListeningEvent.landedAt(): HistorySpot = target() ?: spot()

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
