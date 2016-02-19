package de.ph1b.audiobook.playback

import android.content.Context
import android.content.Intent
import android.view.KeyEvent
import javax.inject.Inject

/**
 * Providing intents that can be sent to the playback service.
 *
 * @author Paul Woitaschek
 */
class ServiceController @Inject constructor(private val context: Context) {

    fun getStopIntent(): Intent {
        val intent = Intent(context, BookReaderService::class.java)
        intent.setAction(Intent.ACTION_MEDIA_BUTTON)
        val keyEvent = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_STOP)
        intent.putExtra(Intent.EXTRA_KEY_EVENT, keyEvent)
        return intent
    }

    fun getPlayPauseIntent(): Intent {
        val intent = Intent(context, BookReaderService::class.java)
        intent.setAction(Intent.ACTION_MEDIA_BUTTON)
        val keyEvent = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
        intent.putExtra(Intent.EXTRA_KEY_EVENT, keyEvent)
        return intent
    }

    fun getFastForwardIntent(): Intent {
        val intent = Intent(context, BookReaderService::class.java)
        intent.setAction(Intent.ACTION_MEDIA_BUTTON)
        val keyEvent = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD)
        intent.putExtra(Intent.EXTRA_KEY_EVENT, keyEvent)
        return intent
    }

    fun getRewindIntent(): Intent {
        val intent = Intent(context, BookReaderService::class.java)
        intent.setAction(Intent.ACTION_MEDIA_BUTTON)
        val keyEvent = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_REWIND)
        intent.putExtra(Intent.EXTRA_KEY_EVENT, keyEvent)
        return intent
    }
}
