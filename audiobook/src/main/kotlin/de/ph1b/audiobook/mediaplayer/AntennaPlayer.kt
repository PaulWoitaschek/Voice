package de.ph1b.audiobook.mediaplayer

import android.content.Context
import org.antennapod.audio.MediaPlayer
import rx.Observable
import timber.log.Timber

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

    override val errorObservable = Observable.defer<Unit> {
        Observable.create { subscriber ->
            mediaPlayer.setOnErrorListener { mediaPlayer, what, extra ->
                Timber.e("onError with what=$what and extra=$extra")
                if (subscriber.isUnsubscribed) {
                    false
                } else {
                    subscriber.onNext(Unit)
                    true
                }
            }
        }
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

    override val completionObservable = Observable.defer<Unit> {
        Observable.create { subscriber ->
            mediaPlayer.setOnCompletionListener {
                if (!subscriber.isUnsubscribed) {
                    subscriber.onNext(Unit)
                }
            }
        }
    }
}