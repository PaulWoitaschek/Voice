package voice.features.bookmark.history

import voice.core.data.ChapterId
import voice.core.data.ListeningEvent
import voice.core.data.ListeningEvent.Source
import voice.core.data.ListeningEvent.Type
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
    // the playing stretch is the newest one, so it's the first of the first day
    val today = days.first()
    val playing = today.stretches.first()
    val to = current?.let { index.historyLocation(it.chapterId, it.time) } ?: playing.to
    val updated = playing.copy(end = now.localTime(zone), listened = ongoing.listened(now.toEpochMilli()), to = to)
    return HistoryViewState(listOf(today.copy(stretches = listOf(updated) + today.stretches.drop(1))) + days.drop(1))
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
  val raw = splitStretches(events.sortedWith(compareBy({ it.atMillis }, { it.id })))
  val ongoing = raw.lastOrNull()?.takeIf { playing && it.openSince != null }
  raw.filter { it !== ongoing }.forEach { it.closeLostPause() }
  val stretches = raw
    .filter {
      it === ongoing || it.endUnknown || it.cause == StretchCause.SleepTimerStarted ||
        it.closedMillis >= MIN_STRETCH.inWholeMilliseconds
    }
    .mapNotNull { stretch -> stretch.toStretch(index, zone, isOngoing = stretch === ongoing)?.let { stretch.startMillis to it } }
    .reversed()
  val days = mutableListOf<HistoryDay>()
  stretches.forEach { (startMillis, stretch) ->
    val label = sessionLabel(Instant.ofEpochMilli(startMillis), now, zone)
    val last = days.lastOrNull()
    if (last != null && last.label == label) {
      days[days.lastIndex] = last.copy(stretches = last.stretches + stretch)
    } else {
      days += HistoryDay(label, listOf(stretch))
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

/**
 * Splits the events, oldest first, into stretches of listening. A pause of [STRETCH_GAP] or more
 * starts a new one, and so does a big jump while playing, so the place before an accidental jump
 * is where the stretch before it ends.
 */
private fun splitStretches(events: List<ListeningEvent>): List<RawStretch> {
  val stretches = mutableListOf<RawStretch>()
  var current: RawStretch? = null
  // a big jump while paused starts the next stretch, once playback goes on
  var pendingJump: ListeningEvent? = null

  fun open(
    event: ListeningEvent,
    from: HistorySpot,
    cause: StretchCause,
  ) {
    val stretch = RawStretch(key = event.id, startMillis = event.atMillis, from = from, cause = cause)
    stretch.openSince = event.atMillis
    stretches += stretch
    current = stretch
  }

  events.forEach { event ->
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
          open(event, event.spot(), if (jump != null) StretchCause.Jumped(jump.type, jump.source) else StretchCause.Played(event.source))
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
        if (stretch != null && stretch.openSince != null) {
          stretch.stop(event.atMillis, event.spot())
          open(event, event.landedAt(), StretchCause.Jumped(event.type, event.source))
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
  return stretches
}

private fun RawStretch.toStretch(
  index: BookIndex,
  zone: ZoneId,
  isOngoing: Boolean,
): HistoryStretch? {
  val from = index.historyLocation(from.chapterId, from.time) ?: return null
  val to = to?.let { index.historyLocation(it.chapterId, it.time) }
  val unknownEnd = endUnknown || cause == StretchCause.SleepTimerStarted
  // after falling asleep, playback is at the end already, so this goes to where the listener was still awake
  val target = if (endedBySleepTimer || unknownEnd) lastTouch else this.to
  return HistoryStretch(
    key = key,
    start = Instant.ofEpochMilli(startMillis).localTime(zone),
    end = if (unknownEnd) null else Instant.ofEpochMilli(endMillis).localTime(zone),
    listened = if (unknownEnd) null else closedMillis.milliseconds,
    from = from,
    to = to.takeUnless { unknownEnd },
    cause = if (endedBySleepTimer) StretchCause.SleepTimerEnded else cause,
    goTo = target?.takeIf { !isOngoing && index.locate(it.chapterId, it.time) != null },
  )
}

private fun ListeningEvent.spot(): HistorySpot = HistorySpot(chapterId, time)

private fun ListeningEvent.landedAt(): HistorySpot {
  val toChapterId = toChapterId
  return if (toChapterId != null) HistorySpot(toChapterId, toTime ?: 0L) else spot()
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
