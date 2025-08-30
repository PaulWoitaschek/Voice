package voice.core.playback.playstate

import androidx.media3.common.Player
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import voice.core.data.repo.BookRepository
import voice.core.logging.core.Logger
import voice.core.playback.session.MediaId
import voice.core.playback.session.toMediaIdOrNull
import kotlin.time.Duration.Companion.milliseconds

@Inject
class PositionUpdater(
  private val bookRepo: BookRepository,
  private val scope: CoroutineScope,
  private val playStateManager: PlayStateManager,
) : Player.Listener {

  private lateinit var player: Player

  fun attachTo(player: Player) {
    this.player = player
    player.addListener(this)

    scope.launch {
      playStateManager.flow
        .map { it == PlayStateManager.PlayState.Playing }
        .distinctUntilChanged()
        .collectLatest { playing ->
          if (playing) {
            while (true) {
              updatePosition()
              delay(400.milliseconds)
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
    Logger.v("onPositionDiscontinuity: ${newPosition.positionMs}")
    scope.launch {
      updatePosition()
    }
  }

  private suspend fun updatePosition() {
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
}
