package de.ph1b.audiobook.playback.player

import android.content.Context
import android.os.Handler
import android.os.Looper
import org.antennapod.audio.SonicAudioPlayer
import java.io.File

/**
 * Implementation of the player using AntennaPods AudioPlayer internally
 *
 * @author Paul Woitaschek
 */
class AntennaPlayer(private val context: Context) : Player() {

    private val player: SonicAudioPlayer
    private val handler = Handler(context.mainLooper)

    private inline fun postOnMain(crossinline task: () -> Unit) {
        if (Thread.currentThread() == Looper.getMainLooper().thread) {
            task()
        } else handler.post { task() }
    }

    override fun setVolume(volume: Float) = player.setVolume(volume, volume)

    init {
        val owning = org.antennapod.audio.MediaPlayer(context)
        player = SonicAudioPlayer(owning, context)

        owning.setOnErrorListener { mediaPlayer, i, j ->
            postOnMain { errorSubject.onNext(Unit) }
            false
        }
        owning.setOnCompletionListener { postOnMain { completionSubject.onNext(Unit) } }
        owning.setOnPreparedListener { postOnMain { preparedSubject.onNext(Unit) } }
    }


    override fun seekTo(to: Int) = player.seekTo(to)

    override fun isPlaying() = player.isPlaying

    override fun start() = player.start()

    override fun pause() = player.pause()

    override fun prepare(file: File) {
        player.setDataSource(file.absolutePath)
        player.prepare()
    }

    override fun reset() = player.reset()

    override fun setWakeMode(mode: Int) = player.setWakeMode(context, mode)

    override fun setAudioStreamType(streamType: Int) = player.setAudioStreamType(streamType)

    override val currentPosition: Int
        get() = player.currentPosition

    override val duration: Int
        get() = player.duration

    override var playbackSpeed: Float
        get() = player.currentSpeedMultiplier
        set(value) = player.setPlaybackSpeed(value)
}