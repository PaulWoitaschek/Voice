package de.ph1b.audiobook.mediaplayer

import android.content.Context
import org.antennapod.audio.MediaPlayer

/**
 * Simple delegate on AntennaPods MediaPlayer
 *
 * @author Paul Woitaschek
 */
class AntennaPlayer(private val context: Context) : MediaPlayerInterface {

    override var currentPosition: Int
        get() = mediaPlayer.currentPosition
        set(value) {
            mediaPlayer.seekTo(value)
        }


    override var playbackSpeed: Float
        get() = mediaPlayer.currentSpeedMultiplier
        set(value) {
            mediaPlayer.setPlaybackSpeed(value)
        }

    override fun setWakeMode(context: Context, mode: Int) {
        mediaPlayer.setWakeMode(context, mode)
    }

    override val duration: Int
        get() = mediaPlayer.duration

    override fun setOnErrorListener(onErrorListener: android.media.MediaPlayer.OnErrorListener) {
        mediaPlayer.setOnErrorListener { mediaPlayer, what, extra -> onErrorListener.onError(null, what, extra) }
    }

    val mediaPlayer = object : MediaPlayer(context, false) {
        override fun useSonic(): Boolean {
            return true
        }
    }

    override fun release() {
        mediaPlayer.release()
    }

    override fun start() {
        mediaPlayer.start()
    }

    override fun reset() {
        mediaPlayer.reset()
    }

    override fun prepare() {
        mediaPlayer.prepare()
    }

    override fun pause() {
        mediaPlayer.pause()
    }

    override fun setDataSource(source: String) {
        mediaPlayer.setDataSource(source)
    }

    override fun setOnCompletionListener(onCompletionListener: MediaPlayerInterface.OnCompletionListener) {
        mediaPlayer.setOnCompletionListener({ onCompletionListener.onCompletion() })
    }
}