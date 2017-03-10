package de.ph1b.audiobook.playback

import android.net.Uri
import de.paul_woitaschek.mediaplayer.MediaPlayer
import i
import java.io.File
import kotlin.concurrent.thread

/**
 * Class for managing another [MediaPlayer] that can be swapped
 *
 * @author Paul Woitaschek
 */
class NextPlayer(private var player: MediaPlayer) {

  private var preparingThread: Thread? = null

  private var fileToPrepare: File? = null

  @Volatile private var state: State = State.IDLE
    private set

  // the player is ready to be swapped
  fun ready() = !(preparingThread?.isAlive ?: false)

  // swaps the players and prepares the new one. Make sure to check ready() first
  fun swap(nextPlayer: MediaPlayer, newFileToPrepare: File?): PlayerWithState {
    check(ready()) { "still preparing" }

    // release callbacks before setting the old player
    player.onPrepared(null)
    player.onError(null)
    player.onCompletion(null)

    // fire a new thread as what's actually time consuming is the call to .reset() on the current player
    preparingThread = thread {
      // set future player
      this.player = nextPlayer
      fileToPrepare = newFileToPrepare

      // reset and set error callback
      player.reset()
      state = State.IDLE

      // prepare if requested
      if (newFileToPrepare != null) {
        i { "prepare new file $newFileToPrepare" }
        player.onError {
          player.reset()
          state = State.IDLE
        }
        player.prepare(Uri.fromFile(newFileToPrepare))
        state = State.PREPARED
      }
    }
    return PlayerWithState(player, state == State.PREPARED, fileToPrepare)
  }

  data class PlayerWithState(val player: MediaPlayer, val ready: Boolean, val preparedFile: File?)

  enum class State {
    IDLE,
    PREPARED
  }
}