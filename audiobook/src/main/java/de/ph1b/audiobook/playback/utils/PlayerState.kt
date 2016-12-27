package de.ph1b.audiobook.playback.utils

import com.google.android.exoplayer2.ExoPlayer

/**
 * The states the player can hav
 *
 * @author Paul Woitaschek
 */
enum class PlayerState {

  IDLE,
  PAUSED,
  PLAYING,
  ENDED;

  companion object {
    /** maps the state that comes results from ExoPlayer.EventListener.onPlayerStateChanged */
    fun byExoState(playWhenReady: Boolean, playbackState: Int) = when (playbackState) {
      ExoPlayer.STATE_ENDED -> ENDED
      ExoPlayer.STATE_IDLE -> IDLE
      else -> if (playWhenReady) PLAYING else PAUSED
    }
  }
}