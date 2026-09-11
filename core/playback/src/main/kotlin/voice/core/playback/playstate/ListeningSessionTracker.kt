package voice.core.playback.playstate

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import voice.core.data.BookId
import voice.core.data.ListeningSession
import voice.core.data.repo.ListeningSessionRepo
import voice.core.logging.api.Logger
import voice.core.playback.di.PlaybackScope
import voice.core.playback.session.bookId
import voice.core.playback.session.toMediaIdOrNull
import java.time.Clock
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

// Maps playback state into aggregatable local listening segments, keeping stats logic out of the position updater.
@Inject
@SingleIn(PlaybackScope::class)
class ListeningSessionTracker(
  private val repo: ListeningSessionRepo,
  private val scope: CoroutineScope,
  private val playStateManager: PlayStateManager,
  private val clock: Clock,
) : Player.Listener {

  private var player: Player? = null
  private var stateJob: Job? = null
  private var checkpointJob: Job? = null
  private var activeSegment: ActiveSegment? = null
  private val mutex = Mutex()

  // Very short accidental plays must not pollute stats; a three-second threshold filters most of them out.
  private val minimumSessionDurationMs = 3.seconds.inWholeMilliseconds

  // Checkpoints reduce the risk of losing listening time if the process is killed.
  private val checkpointInterval = 30.seconds

  // The tracker follows the player lifecycle, keeping stats independent of playback-position persistence.
  fun attachTo(player: Player) {
    this.player?.removeListener(this)
    this.player = player
    player.addListener(this)
    stateJob = scope.launch {
      playStateManager.playStateFlow
        .collect { playState ->
          when (playState) {
            PlayStateManager.PlayState.Playing -> startOrSwitchToCurrentBook()
            PlayStateManager.PlayState.Paused -> finishActiveSegment()
          }
        }
    }
  }

  // A media transition can cross books, so the previous segment must be closed before opening a new one.
  override fun onMediaItemTransition(
    mediaItem: MediaItem?,
    reason: Int,
  ) {
    if (playStateManager.playState == PlayStateManager.PlayState.Playing) {
      scope.launch {
        startOrSwitchToCurrentBook()
      }
    }
  }

  // Settle immediately when the player ends or goes idle instead of waiting for async play-state propagation.
  override fun onPlaybackStateChanged(playbackState: Int) {
    if (playbackState == Player.STATE_IDLE || playbackState == Player.STATE_ENDED) {
      scope.launch {
        finishActiveSegment()
      }
    }
  }

  // Synchronous flush hook before release so the service never loses the final segment.
  suspend fun flushNow() {
    finishActiveSegment()
  }

  // Stop listening and cancel background checkpoints so a recreated service does not double-collect.
  fun release() {
    player?.removeListener(this)
    stateJob?.cancel()
    checkpointJob?.cancel()
  }

  private suspend fun startOrSwitchToCurrentBook() {
    mutex.withLock {
      val bookId = currentBookId() ?: return
      val currentSegment = activeSegment
      if (currentSegment?.bookId == bookId) return
      val now = clock.instant()
      if (currentSegment != null) {
        activeSegment = persistSegment(currentSegment, now, keepOpen = false)
      }
      activeSegment = ActiveSegment(
        id = ListeningSession.Id.random(),
        bookId = bookId,
        startedAt = now,
      )
      restartCheckpointJob()
    }
  }

  private suspend fun finishActiveSegment() {
    mutex.withLock {
      val currentSegment = activeSegment ?: return
      activeSegment = persistSegment(currentSegment, clock.instant(), keepOpen = false)
      checkpointJob?.cancel()
    }
  }

  private fun restartCheckpointJob() {
    checkpointJob?.cancel()
    checkpointJob = scope.launch {
      // Periodic checkpoints only act as a safety net and never decide final semantics.
      while (true) {
        delay(checkpointInterval)
        checkpointActiveSegment()
      }
    }
  }

  private suspend fun checkpointActiveSegment() {
    mutex.withLock {
      val currentSegment = activeSegment ?: return
      activeSegment = persistSegment(currentSegment, clock.instant(), keepOpen = true)
    }
  }

  private suspend fun persistSegment(
    segment: ActiveSegment,
    endedAt: Instant,
    keepOpen: Boolean,
  ): ActiveSegment? {
    // Backward or zero-length intervals are ignored so clock jitter never pollutes history.
    if (!endedAt.isAfter(segment.startedAt)) return segment.takeIf { keepOpen }

    var currentStart = segment.startedAt
    var currentId = segment.id
    var nextBoundary = nextLocalDayStart(currentStart)
    // Split across day boundaries into natural-day segments so the review page can aggregate by day.
    while (nextBoundary.isBefore(endedAt)) {
      putIfLongEnough(segment.bookId, currentStart, nextBoundary, currentId)
      currentStart = nextBoundary
      currentId = ListeningSession.Id.random()
      nextBoundary = nextLocalDayStart(currentStart)
    }

    putIfLongEnough(segment.bookId, currentStart, endedAt, currentId)
    return if (keepOpen) {
      ActiveSegment(
        id = currentId,
        bookId = segment.bookId,
        startedAt = currentStart,
      )
    } else {
      null
    }
  }

  private suspend fun putIfLongEnough(
    bookId: BookId,
    startedAt: Instant,
    endedAt: Instant,
    id: ListeningSession.Id,
  ) {
    val durationMs = endedAt.toEpochMilli() - startedAt.toEpochMilli()
    // Accidental or very short playback is not recorded, so opening a menu and leaving instantly leaves no dirty data.
    if (durationMs < minimumSessionDurationMs) return
    val session = ListeningSession(
      id = id,
      bookId = bookId,
      startedAt = startedAt,
      endedAt = endedAt,
      durationMs = durationMs,
    )
    repo.put(session)
    Logger.v("Updated listening session=$session")
  }

  private fun nextLocalDayStart(instant: Instant): Instant {
    return instant.atZone(clock.zone)
      .toLocalDate()
      .plusDays(1)
      .atStartOfDay(clock.zone)
      .toInstant()
  }

  private fun currentBookId(): BookId? {
    return player
      ?.currentMediaItem
      ?.mediaId
      ?.toMediaIdOrNull()
      ?.bookId
  }

  private data class ActiveSegment(
    val id: ListeningSession.Id,
    val bookId: BookId,
    val startedAt: Instant,
  )
}
