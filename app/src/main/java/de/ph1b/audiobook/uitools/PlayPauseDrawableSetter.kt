package de.ph1b.audiobook.uitools

import android.graphics.drawable.AnimatedVectorDrawable
import com.google.android.material.floatingactionbutton.FloatingActionButton
import de.ph1b.audiobook.R
import de.ph1b.audiobook.misc.drawable

class PlayPauseDrawableSetter(val fab: FloatingActionButton) {

  private val playToPause =
    fab.context.drawable(R.drawable.avd_play_to_pause) as AnimatedVectorDrawable
  private val pauseToPlay =
    fab.context.drawable(R.drawable.avd_pause_to_play) as AnimatedVectorDrawable

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
