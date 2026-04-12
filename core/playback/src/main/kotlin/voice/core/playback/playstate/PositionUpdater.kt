package voice.core.playback.playstate

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import voice.core.data.repo.BookRepository
import voice.core.featureflag.ExperimentalPlaybackPersistenceQualifier
import voice.core.featureflag.FeatureFlag
import voice.core.logging.api.Logger
import voice.core.playback.di.PlaybackScope
import voice.core.playback.session.MediaId
import voice.core.playback.session.toMediaIdOrNull
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Inject
@SingleIn(PlaybackScope::class)
class PositionUpdater(
  private val bookRepo: BookRepository,
  private val scope: CoroutineScope,
  private val playStateManager: PlayStateManager,
  @ExperimentalPlaybackPersistenceQualifier
  private val experimentalPlaybackPersistenceFeatureFlag: FeatureFlag<Boolean>,
) : Player.Listener {

  private var player: Player? = null
  private var updateJob: Job? = null

  fun attachTo(player: Player) {
    Logger.d("attachTo $player, $this")
    this.player?.removeListener(this)
    this.player = player
    player.addListener(this)

    updateJob = scope.launch {
      playStateManager.flow
        .map { it == PlayStateManager.PlayState.Playing }
        .distinctUntilChanged()
        .collectLatest { playing ->
          if (playing) {
            while (true) {
              delay(
                if (experimentalPlaybackPersistenceFeatureFlag.get()) {
                  10.seconds
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
    if (!playWhenReady) {
      flushPosition()
    }
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
    val currentPosition = player.currentPosition
      .takeIf { it >= 0 } ?: return
    val mediaId = mediaItem.mediaId.toMediaIdOrNull() ?: return
    mediaId as MediaId.Chapter
    val chapterId = mediaId.chapterId
    bookRepo.updateBook(mediaId.bookId) { content ->
      if (chapterId in content.chapters) {
        Logger.d("$currentPosition is the new position!")
        content.copy(
          currentChapter = chapterId,
          positionInChapter = currentPosition,
        )
      } else {
        Logger.w("$mediaId not in $content")
        content
      }
    }
  }

  fun release() {
    player?.removeListener(this)
    updateJob?.cancel()
  }
}
