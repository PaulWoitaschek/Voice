package voice.playback.playstate

import androidx.media3.common.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import voice.data.repo.BookRepository
import voice.playback.session.MediaId
import voice.playback.session.toMediaIdOrNull
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

class PositionUpdater
@Inject constructor(
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

  override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
    scope.launch {
      updatePosition()
    }
  }

  private suspend fun updatePosition() {
    val mediaItem = player.currentMediaItem ?: return
    val mediaId = mediaItem.mediaId.toMediaIdOrNull() ?: return
    mediaId as MediaId.Chapter
    bookRepo.updateBook(mediaId.bookId) { content ->
      content.copy(
        currentChapter = mediaId.chapterId,
        positionInChapter = player.currentPosition,
      )
    }
  }
}
