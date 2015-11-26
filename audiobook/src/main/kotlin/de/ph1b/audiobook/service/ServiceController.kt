package de.ph1b.audiobook.service

import android.content.Context
import android.content.Intent
import android.view.KeyEvent

import java.io.File
import javax.inject.Inject

/**
 * @author [Paul Woitaschek](mailto:woitaschek@posteo.de)
 * *
 * @link {http://www.paul-woitaschek.de}
 * *
 * @see [http://www.paul-woitaschek.de](http://www.paul-woitaschek.de)
 */
class ServiceController @Inject constructor(private val context: Context) {

    fun setPlaybackSpeed(speed: Float) {
        val i = Intent(context, BookReaderService::class.java)
        i.setAction(CONTROL_SET_PLAYBACK_SPEED)
        i.putExtra(CONTROL_SET_PLAYBACK_SPEED_EXTRA_SPEED, speed)
        context.startService(i)
    }

    fun changeTime(time: Int, file: File) {
        val intent = Intent(context, BookReaderService::class.java)
        intent.setAction(CONTROL_CHANGE_POSITION)
        intent.putExtra(CONTROL_CHANGE_POSITION_EXTRA_TIME, time)
        intent.putExtra(CONTROL_CHANGE_POSITION_EXTRA_FILE, file)
        context.startService(intent)
    }

    fun playPause() {
        context.startService(getPlayPauseIntent(context))
    }

    fun fastForward() {
        context.startService(getFastForwardIntent(context))
    }

    fun rewind() {
        context.startService(getRewindIntent(context))
    }

    operator fun next() {
        val intent = Intent(context, BookReaderService::class.java)
        intent.setAction(CONTROL_NEXT)
        context.startService(intent)
    }

    fun previous() {
        val intent = Intent(context, BookReaderService::class.java)
        intent.setAction(CONTROL_PREVIOUS)
        context.startService(intent)
    }

    fun toggleSleepSand() {
        val intent = Intent(context, BookReaderService::class.java)
        intent.setAction(CONTROL_TOGGLE_SLEEP_SAND)
        context.startService(intent)
    }


    companion object {
        val CONTROL_SET_PLAYBACK_SPEED = "CONTROL_SET_PLAYBACK_SPEED"
        val CONTROL_SET_PLAYBACK_SPEED_EXTRA_SPEED = "CONTROL_SET_PLAYBACK_SPEED_EXTRA_SPEED"

        val CONTROL_TOGGLE_SLEEP_SAND = "CONTROL_TOGGLE_SLEEP_SAND"
        val CONTROL_CHANGE_POSITION = "CONTROL_CHANGE_POSITION"
        val CONTROL_CHANGE_POSITION_EXTRA_TIME = "CONTROL_CHANGE_POSITION_EXTRA_TIME"
        val CONTROL_CHANGE_POSITION_EXTRA_FILE = "CONTROL_CHANGE_POSITION_EXTRA_FILE"
        val CONTROL_NEXT = "CONTROL_NEXT"
        val CONTROL_PREVIOUS = "CONTROL_PREVIOUS"

        fun getStopIntent(c: Context): Intent {
            val intent = Intent(c, BookReaderService::class.java)
            intent.setAction(Intent.ACTION_MEDIA_BUTTON)
            val keyEvent = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_STOP)
            intent.putExtra(Intent.EXTRA_KEY_EVENT, keyEvent)
            return intent
        }

        fun getPlayPauseIntent(c: Context): Intent {
            val intent = Intent(c, BookReaderService::class.java)
            intent.setAction(Intent.ACTION_MEDIA_BUTTON)
            val keyEvent = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
            intent.putExtra(Intent.EXTRA_KEY_EVENT, keyEvent)
            return intent
        }

        fun getFastForwardIntent(c: Context): Intent {
            val intent = Intent(c, BookReaderService::class.java)
            intent.setAction(Intent.ACTION_MEDIA_BUTTON)
            val keyEvent = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD)
            intent.putExtra(Intent.EXTRA_KEY_EVENT, keyEvent)
            return intent
        }

        fun getRewindIntent(c: Context): Intent {
            val intent = Intent(c, BookReaderService::class.java)
            intent.setAction(Intent.ACTION_MEDIA_BUTTON)
            val keyEvent = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_REWIND)
            intent.putExtra(Intent.EXTRA_KEY_EVENT, keyEvent)
            return intent
        }
    }
}
