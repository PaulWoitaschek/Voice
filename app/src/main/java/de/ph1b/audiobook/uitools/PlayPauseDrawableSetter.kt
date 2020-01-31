package de.ph1b.audiobook.uitools

import android.graphics.drawable.AnimatedVectorDrawable
import android.widget.ImageView
import de.ph1b.audiobook.R

class PlayPauseDrawableSetter(val fab: ImageView) {

  private val playToPause =
    fab.context.getDrawable(R.drawable.avd_play_to_pause)!! as AnimatedVectorDrawable
  private val pauseToPlay =
    fab.context.getDrawable(R.drawable.avd_pause_to_play)!! as AnimatedVectorDrawable

  private var playing = false

  init {
    fab.setImageDrawable(playToPause)
  }

  fun setPlaying(playing: Boolean, animated: Boolean) {
    if (this.playing == playing) {
      return
    }
    this.playing = playing

    if (animated) {
      val drawable = if (playing) {
        playToPause
      } else {
        pauseToPlay
      }
      fab.setImageDrawable(drawable)
      drawable.start()
    } else {
      val drawable = if (playing) {
        pauseToPlay
      } else {
        playToPause
      }
      fab.setImageDrawable(drawable)
    }
  }
}
