package voice.playback.player

import androidx.datastore.core.DataStore
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import de.paulwoitaschek.flowpref.Pref
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import voice.common.BookId
import voice.common.pref.CurrentBook
import voice.common.pref.PrefKeys
import voice.data.repo.BookRepository
import voice.playback.session.chapterMarks
import java.time.Instant
import javax.inject.Inject
import javax.inject.Named
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class VoicePlayer
@Inject constructor(
  private val player: Player,
  private val repo: BookRepository,
  @CurrentBook
  private val currentBookId: DataStore<BookId?>,
  @Named(PrefKeys.SEEK_TIME)
  private val seekTimePref: Pref<Int>,
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
    /**
     * On Android 13, the notification always shows the "skip to next" and "skip to previous"
     * actions.
     * However these are also used internally when seeking for example through a bluetooth headset
     * We use these and delegate them to fast forward / rewind.
     * The player however only advertises the seek to next and previous item in the case
     * that it's not the first or last track. Therefore we manually advertise that these
     * are available.
     */
    return super.getAvailableCommands()
      .buildUpon()
      .addAll(
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

  override fun seekForward() {
    seek(forward = true)
  }

  override fun seekBack() {
    seek(forward = false)
  }

  private fun seek(forward: Boolean) {
    val rawAmount = seekTimePref.value.seconds
    val skipAmount = if (forward) {
      rawAmount
    } else {
      -rawAmount
    }

    val currentPos = player.currentPosition.takeUnless { it == C.TIME_UNSET }
      ?.milliseconds
      ?.coerceAtLeast(Duration.ZERO)
      ?: return
    val duration = player.duration.takeUnless { it == C.TIME_UNSET }
      ?.milliseconds
      ?: return

    val seekTo = currentPos + skipAmount
    when {
      seekTo < Duration.ZERO -> forceSeekToPrevious()
      seekTo > duration -> forceSeekToNext()
      else -> seekTo(seekTo.inWholeMilliseconds)
    }
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
