package de.ph1b.audiobook.mediaplayer

import android.content.Context
import android.media.MediaPlayer

import java.io.IOException


/**
 * Simple delegate on the Androids MediaPlayer
 */
class AndroidMediaPlayer : MediaPlayerInterface {
    override var currentPosition: Int
        get() = mediaPlayer.currentPosition
        set(value) {
            mediaPlayer.seekTo(value)
        }

    override var playbackSpeed: Float
        get() = 1f
        set(value) {
            // ignore as android media-player is not capable of this
        }

    override val duration: Int
        get() = mediaPlayer.duration

    private val mediaPlayer = MediaPlayer()

    override fun release() {
        mediaPlayer.release()
    }

    override fun start() {
        mediaPlayer.start()
    }

    override fun reset() {
        mediaPlayer.reset()
    }

    @Throws(IOException::class)
    override fun prepare() {
        mediaPlayer.prepare()
    }

    override fun pause() {
        mediaPlayer.pause()
    }

    @Throws(IOException::class)
    override fun setDataSource(source: String) {
        mediaPlayer.setDataSource(source)
    }

    override fun setOnErrorListener(onErrorListener: MediaPlayer.OnErrorListener) {
        mediaPlayer.setOnErrorListener(onErrorListener)
    }

    override fun setOnCompletionListener(onCompletionListener: MediaPlayerInterface.OnCompletionListener) {
        mediaPlayer.setOnCompletionListener { mp -> onCompletionListener.onCompletion() }
    }

    override fun setWakeMode(context: Context, mode: Int) {
        mediaPlayer.setWakeMode(context, mode)
    }
}
