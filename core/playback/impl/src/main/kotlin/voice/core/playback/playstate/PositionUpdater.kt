package voice.core.playback.playstate

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import voice.core.data.repo.BookRepository
import voice.core.logging.api.Logger
import voice.core.playback.ExperimentalPlaybackPersistenceFeatureFlag
import voice.core.playback.PlaybackPersistenceIntervalFeatureFlag
import voice.core.playback.di.PlaybackScope
import voice.core.playback.session.bookId
import voice.core.playback.session.positionInChapter
import voice.core.playback.session.realChapterId
import voice.core.playback.session.toMediaIdOrNull

@Inject
@SingleIn(PlaybackScope::class)
class PositionUpdater(
  private val bookRepo: BookRepository,
  private val scope: CoroutineScope,
  private val playStateManager: PlayStateManager,
  private val experimentalPlaybackPersistenceFeatureFlag:
    ExperimentalPlaybackPersistenceFeatureFlag,
  private val playbackPersistenceIntervalFeatureFlag: PlaybackPersistenceIntervalFeatureFlag,
) : Player.Listener {

  private var player: Player? = null
  private var updateJob: Job? = null

  fun attachTo(player: Player) {
    this.player?.removeListener(this)
    this.player = player
    player.addListener(this)

    updateJob = scope.launch {
      playStateManager.playStateFlow
        .map { it is PlayStateManager.PlayState.Playing }
        .distinctUntilChanged()
        .collectLatest { playing ->
          if (playing) {
            while (true) {
              delay(
                if (experimentalPlaybackPersistenceFeatureFlag.get()) {
                  playbackPersistenceIntervalFeatureFlag.interval()
                } else {
                  400.milliseconds
                },
              )
              flushPositionNow()
            }
          }
        }
    }
  }

  override fun onPositionDiscontinuity(
    oldPosition: Player.PositionInfo,
    newPosition: Player.PositionInfo,
    reason: Int,
  ) {
    flushPosition()
  }

  override fun onPlayWhenReadyChanged(
    playWhenReady: Boolean,
    reason: Int,
  ) {
    flushPosition()
  }

  override fun onPlaybackStateChanged(playbackState: Int) {
    if (playbackState == Player.STATE_IDLE || playbackState == Player.STATE_ENDED) {
      flushPosition()
    }
  }

  override fun onMediaItemTransition(
    mediaItem: MediaItem?,
    reason: Int,
  ) {
    flushPosition()
  }

  private fun flushPosition() {
    scope.launch {
      flushPositionNow()
    }
  }

  suspend fun flushPositionNow() {
    val player = player ?: return
    val mediaItem = player.currentMediaItem ?: return
    val currentPosition = player.currentPosition.takeIf { it >= 0 } ?: return
    val mediaId = mediaItem.mediaId.toMediaIdOrNull() ?: return
    val bookId = mediaId.bookId ?: return
    val chapterId = mediaId.realChapterId ?: return
    val positionInChapter = mediaId.positionInChapter(currentPosition) ?: return
    bookRepo.updateBook(bookId) { content ->
      if (chapterId in content.chapters) {
        content.copy(
          currentChapter = chapterId,
          positionInChapter = positionInChapter,
          lastPlayedAt = Clock.System.now(),
        )
      } else {
        Logger.v("Media item not in the current book")
        content
      }
    }
  }

  fun release() {
    player?.removeListener(this)
    updateJob?.cancel()
  }
}
