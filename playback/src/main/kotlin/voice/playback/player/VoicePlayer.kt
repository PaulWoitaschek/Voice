package voice.playback.player

import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import voice.playback.session.chapterMarks
import javax.inject.Inject

class VoicePlayer
@Inject constructor(
  private val player: Player,
) : ForwardingPlayer(player) {

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

  fun setSkipSilenceEnabled(enabled: Boolean): Boolean {
    return if (player is ExoPlayer) {
      player.skipSilenceEnabled = enabled
      true
    } else {
      false
    }
  }
}
