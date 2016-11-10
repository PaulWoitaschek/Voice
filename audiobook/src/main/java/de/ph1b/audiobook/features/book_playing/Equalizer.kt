package de.ph1b.audiobook.features.book_playing

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import de.ph1b.audiobook.playback.player.Player


/**
 * The equalizer. Delegates to the system integrated equalizer
 *
 * @author Paul Woitaschek
 */
object Equalizer {

    private val intent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
        putExtra(AudioEffect.EXTRA_AUDIO_SESSION, Player.AUDIO_SESSION_ID)
    }

    fun exists(context: Context) = context.packageManager.resolveActivity(intent, 0) != null

    fun launch(activity: Activity) {
        activity.startActivityForResult(intent, 664)
    }
}