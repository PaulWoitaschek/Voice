package de.ph1b.audiobook.mediaplayer

import android.content.Context
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Build
import rx.Observable
import java.io.IOException


/**
 * Simple delegate on the Androids MediaPlayer
 */
class AndroidMediaPlayer : MediaPlayerInterface {

    private val canUsePlaybackParams = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
    private val mediaPlayer: MediaPlayer

    init {
        mediaPlayer = MediaPlayer()
    }

    override var currentPosition: Int
        get() = mediaPlayer.currentPosition
        set(value) {
            mediaPlayer.seekTo(value)
        }

    override var playbackSpeed: Float = 1.0f
        set(value) {
            if (canUsePlaybackParams) {
                mediaPlayer.playbackParams = PlaybackParams().setSpeed(value)
            }
            field = value
        }

    override val duration: Int
        get() = mediaPlayer.duration


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

    override val completionObservable: Observable<Unit> = Observable.defer {
        Observable.create<Unit> { subscriber ->
            mediaPlayer.setOnCompletionListener {
                if (!subscriber.isUnsubscribed) {
                    subscriber.onNext(Unit)
                }
            }
        }
    }

    override fun setWakeMode(context: Context, mode: Int) {
        mediaPlayer.setWakeMode(context, mode)
    }
}
