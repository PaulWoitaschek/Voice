package voice.core.playback.stats

import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import dev.zacsweers.metro.AppScope
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
import voice.core.data.repo.ListeningStatsRepo
import voice.core.playback.session.bookId
import voice.core.playback.session.toMediaIdOrNull
import java.time.Clock
import java.time.Instant
import kotlin.math.roundToLong
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Records the listening sessions for the listening stats, from what the player does.
 *
 * While playing, the session is saved every now and then, so little is lost when the app gets killed.
 */
@SingleIn(AppScope::class)
@Inject
class ListeningSessionRecorder(
  private val repo: ListeningStatsRepo,
  private val clock: Clock,
  private val scope: CoroutineScope,
) {

  private var session: OpenSession? = null
  private var playing = false
  private var speed = 1F

  // the listening time is counted up to here
  private var countedUntil = 0L
  private var checkpoints: Job? = null
  private val writes = Mutex()

  fun attachTo(player: Player) {
    player.addListener(
      object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
          if (isPlaying) {
            val bookId = player.currentMediaItem?.bookId() ?: return
            playing(bookId, player.playbackParameters.speed)
          } else {
            stopped()
          }
        }

        override fun onMediaItemTransition(
          mediaItem: MediaItem?,
          reason: Int,
        ) {
          if (!player.isPlaying) return
          val bookId = mediaItem?.bookId() ?: return
          playing(bookId, player.playbackParameters.speed)
        }

        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
          speedChanged(playbackParameters.speed)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
          if (playbackState == Player.STATE_ENDED) {
            val bookId = player.currentMediaItem?.bookId() ?: return
            bookEnded(bookId)
          }
        }
      },
    )
  }

  fun playing(
    bookId: BookId,
    speed: Float,
  ) {
    if (playing && session?.bookId == bookId) {
      speedChanged(speed)
      return
    }
    stopped()
    val now = clock.millis()
    val previous = session
    session = if (previous != null && previous.bookId == bookId && now - previous.endedAtMillis <= CONTINUE_WITHIN) {
      previous
    } else {
      OpenSession(
        bookId = bookId,
        startedAtMillis = now,
        utcOffsetSeconds = clock.zone.rules.getOffset(Instant.ofEpochMilli(now)).totalSeconds,
      )
    }
    this.speed = speed
    playing = true
    countedUntil = now
    checkpoints = scope.launch {
      while (true) {
        delay(CHECKPOINT_INTERVAL)
        count()
        save()
      }
    }
  }

  fun speedChanged(speed: Float) {
    // what played so far played at the old speed
    count()
    this.speed = speed
  }

  fun stopped() {
    if (!playing) return
    count()
    playing = false
    checkpoints?.cancel()
    checkpoints = null
    save()
  }

  fun bookEnded(bookId: BookId) {
    val session = session ?: return
    // A book that is opened at its end was not finished just now, and neither was the last one listened to.
    val justListened = playing || clock.millis() - session.endedAtMillis <= CONTINUE_WITHIN
    if (session.bookId != bookId || !justListened) return
    count()
    session.reachedEnd = true
    save()
  }

  private fun count() {
    if (!playing) return
    val session = session ?: return
    val now = clock.millis()
    // the clock can be changed, so only count what fits in between two checkpoints
    val elapsed = (now - countedUntil).coerceIn(0, MAX_COUNTED_AT_ONCE)
    session.listenedMillis += elapsed
    session.audioMillis += (elapsed * speed).roundToLong()
    session.endedAtMillis = session.endedAtMillis.coerceAtLeast(now)
    countedUntil = now
  }

  private fun save() {
    val session = session ?: return
    if (session.listenedMillis == 0L && !session.reachedEnd) return
    val snapshot = session.toListeningSession()
    scope.launch {
      writes.withLock {
        // the first save gives the session its id
        session.id = repo.save(snapshot.copy(id = session.id))
      }
    }
  }

  private class OpenSession(
    val bookId: BookId,
    val startedAtMillis: Long,
    val utcOffsetSeconds: Int,
  ) {
    var endedAtMillis = startedAtMillis
    var listenedMillis = 0L
    var audioMillis = 0L
    var reachedEnd = false
    var id = 0L

    fun toListeningSession() = ListeningSession(
      bookId = bookId,
      startedAtMillis = startedAtMillis,
      endedAtMillis = endedAtMillis,
      listenedMillis = listenedMillis,
      audioMillis = audioMillis,
      utcOffsetSeconds = utcOffsetSeconds,
      reachedEnd = reachedEnd,
      id = id,
    )
  }

  private companion object {
    val CHECKPOINT_INTERVAL = 30.seconds
    val MAX_COUNTED_AT_ONCE = 2.minutes.inWholeMilliseconds

    // a short break, like answering a question, doesn't end the session
    val CONTINUE_WITHIN = 5.minutes.inWholeMilliseconds
  }
}

private fun MediaItem.bookId(): BookId? = mediaId.toMediaIdOrNull()?.bookId
