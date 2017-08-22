package de.ph1b.audiobook.playback

import android.content.Context
import android.content.Intent
import android.support.v4.content.ContextCompat.startForegroundService
import android.view.KeyEvent
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Class for controlling the player through the service
 */
@Singleton class PlayerController
@Inject constructor(val context: Context) {

  private val playPauseIntent = keyEventIntent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
  private val playIntent = keyEventIntent(KeyEvent.KEYCODE_MEDIA_PLAY)
  private val stopIntent = keyEventIntent(KeyEvent.KEYCODE_MEDIA_STOP)
  private val nextIntent = intent(ACTION_FORCE_NEXT)
  private val previousIntent = intent(ACTION_FORCE_PREVIOUS)
  private val rewindIntent = keyEventIntent(KeyEvent.KEYCODE_MEDIA_REWIND)
  private val fastForwardIntent = keyEventIntent(KeyEvent.KEYCODE_MEDIA_FAST_FORWARD)

  private fun intent(action: String) = Intent(context, PlaybackService::class.java).apply {
    setAction(action)
  }

  private fun keyEventIntent(keyCode: Int) = intent(Intent.ACTION_MEDIA_BUTTON).apply {
    val keyEvent = KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
    putExtra(Intent.EXTRA_KEY_EVENT, keyEvent)
  }

  fun stop() = fire(stopIntent)

  fun rewind() = fire(rewindIntent)

  fun play() = fire(playIntent)

  fun fastForward() = fire(fastForwardIntent)

  private fun fire(intent: Intent) {
    startForegroundService(context, intent)
  }

  fun previous() = fire(previousIntent)

  fun playPause() = fire(playPauseIntent)

  fun next() = fire(nextIntent)

  fun setSpeed(speed: Float) {
    fire(intent(ACTION_SPEED).apply {
      putExtra(EXTRA_SPEED, speed)
    })
  }

  fun changePosition(time: Int, file: File) {
    fire(intent(ACTION_CHANGE).apply {
      putExtra(CHANGE_TIME, time)
      putExtra(CHANGE_FILE, file.absolutePath)
    })
  }

  fun setLoudnessGain(mB: Int) {
    fire(intent(ACTION_LOUDNESS).apply {
      putExtra(CHANGE_LOUDNESS, mB)
    })
  }

  companion object {
    const val ACTION_SPEED = "action#setSpeed"
    const val ACTION_LOUDNESS = "action#setLoudnessGain"
    const val EXTRA_SPEED = "extra#speed"

    const val ACTION_CHANGE = "action#change"
    const val CHANGE_TIME = "changeTime"
    const val CHANGE_LOUDNESS = "changeLoudness"
    const val CHANGE_FILE = "changeFile"

    const val ACTION_FORCE_NEXT = "actionForceNext"
    const val ACTION_FORCE_PREVIOUS = "actionForcePrevious"
  }
}
