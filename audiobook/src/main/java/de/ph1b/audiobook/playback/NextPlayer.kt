package de.ph1b.audiobook.playback

import android.net.Uri
import de.paul_woitaschek.mediaplayer.MediaPlayer
import e
import i
import java.io.File
import java.io.IOException
import kotlin.concurrent.thread

/**
 * Class for managing another [MediaPlayer] that can be swapped
 *
 * @author Paul Woitaschek
 */
class NextPlayer(private var player: MediaPlayer) {

  private var preparingThread: Thread? = null
  private var fileToPrepare: File? = null
  @Volatile private var prepared = false

  // the player is ready to be swapped
  fun ready() = !(preparingThread?.isAlive ?: false)

  // swaps the players and prepares the new one.
  fun swap(nextPlayer: MediaPlayer, newFileToPrepare: File?): PlayerWithState {
    // if not ready, wait for the thread
    preparingThread?.join()

    // release callbacks before setting the old player
    player.onError(null)
    player.onCompletion(null)
    val playerWithState = PlayerWithState(player, prepared, fileToPrepare)

    // set future player
    player = nextPlayer
    fileToPrepare = newFileToPrepare
    fileToPrepare?.let { prepareAsync(it) }

    return playerWithState
  }

  // fire a new thread as what's actually time consuming is the call to .reset() on the current player
  private fun prepareAsync(file: File) {
    preparingThread = thread {
      // reset and set error callback
      player.reset()
      prepared = false

      // prepare if requested
      i { "prepare new file $file" }
      player.onError {
        e { "onError" }
        player.reset()
        prepared = false
      }
      try {
        player.prepare(Uri.fromFile(file))
        prepared = true
      } catch (e: IOException) {
        e(e) { "Exception while preparing $file async" }
        player.reset()
        prepared = false
      }
    }
  }

  data class PlayerWithState(val player: MediaPlayer, val ready: Boolean, val preparedFile: File?)
}