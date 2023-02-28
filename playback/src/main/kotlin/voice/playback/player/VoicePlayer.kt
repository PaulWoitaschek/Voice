package voice.playback.player

import androidx.datastore.core.DataStore
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import voice.common.BookId
import voice.common.pref.CurrentBook
import voice.data.repo.BookRepository
import voice.playback.session.chapterMarks
import java.time.Instant
import javax.inject.Inject

class VoicePlayer
@Inject constructor(
  private val player: Player,
  private val repo: BookRepository,
  @CurrentBook
  private val currentBookId: DataStore<BookId?>,
) : ForwardingPlayer(player) {

  private val scope = MainScope()

  fun forceSeekToNext() {
    val currentMediaItem = player.currentMediaItem ?: return
    val marks = currentMediaItem.chapterMarks()
    val currentMarkIndex = marks.indexOfFirst { mark ->
      player.currentPosition in mark.startMs..mark.endMs
    }
    val nextMark = marks.getOrNull(currentMarkIndex + 1)
    if (nextMark != null) {
      player.seekTo(nextMark.startMs)
    } else {
      player.seekToNext()
    }
  }

  fun forceSeekToPrevious() {
    val currentMediaItem = player.currentMediaItem ?: return
    val marks = currentMediaItem.chapterMarks()
    val currentPosition = player.currentPosition
    val currentMark = marks.firstOrNull { mark ->
      currentPosition in mark.startMs..mark.endMs
    } ?: marks.last()

    if (currentPosition - currentMark.startMs > 2000) {
      player.seekTo(currentMark.startMs)
    } else {
      val currentMarkIndex = marks.indexOf(currentMark)
      val previousMark = marks.getOrNull(currentMarkIndex - 1)
      if (previousMark != null) {
        player.seekTo(previousMark.startMs)
      } else {
        val currentMediaItemIndex = player.currentMediaItemIndex
        if (currentMediaItemIndex > 0) {
          val previousMediaItemIndex = currentMediaItemIndex - 1
          val previousMediaItem = player.getMediaItemAt(previousMediaItemIndex)
          player.seekTo(previousMediaItemIndex, previousMediaItem.chapterMarks().last().startMs)
        } else {
          player.seekTo(0)
        }
      }
    }
  }

  override fun getAvailableCommands(): Player.Commands {
    return super.getAvailableCommands()
      .buildUpon()
      .removeAll(
        Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
        Player.COMMAND_SEEK_TO_PREVIOUS,
        Player.COMMAND_SEEK_TO_NEXT,
        Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
      )
      .build()
  }

  override fun seekToPreviousMediaItem() {
    seekBack()
  }

  override fun seekToPrevious() {
    seekBack()
  }

  override fun seekToNext() {
    seekForward()
  }

  override fun seekToNextMediaItem() {
    seekForward()
  }

  override fun play() {
    updateLastPlayedAt()
    super.play()
  }

  override fun setPlayWhenReady(playWhenReady: Boolean) {
    if (playWhenReady) {
      updateLastPlayedAt()
    }
    super.setPlayWhenReady(playWhenReady)
  }

  private fun updateLastPlayedAt() {
    scope.launch {
      currentBookId.data.first()?.let { bookId ->
        repo.updateBook(bookId) {
          it.copy(lastPlayedAt = Instant.now())
        }
      }
    }
  }

  override fun getPlaybackState(): Int = when (val state = super.getPlaybackState()) {
    // redirect buffering to ready to prevent visual artifacts on seeking
    Player.STATE_BUFFERING -> Player.STATE_READY
    else -> state
  }

  fun setSkipSilenceEnabled(enabled: Boolean): Boolean {
    return if (player is ExoPlayer) {
      player.skipSilenceEnabled = enabled
      true
    } else {
      false
    }
  }
}
