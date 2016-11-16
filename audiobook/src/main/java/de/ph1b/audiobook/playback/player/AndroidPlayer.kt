package de.ph1b.audiobook.playback.player

import android.content.Context
import java.io.File
import android.media.MediaPlayer as AndroidMediaPlayer


/**
 * Delegates to android.media.MediaPlayer.
 *
 * @author Paul Woitaschek
 */
class AndroidPlayer(private val context: Context) : Player() {

  private val player = AndroidMediaPlayer()

  init {
    player.setOnErrorListener { mediaPlayer, i, j ->
      errorSubject.onNext(Unit)
      false
    }
    player.audioSessionId = Player.AUDIO_SESSION_ID

    player.setOnCompletionListener { completionSubject.onNext(Unit) }
    player.setOnPreparedListener { preparedSubject.onNext(Unit) }
  }

  override fun setVolume(volume: Float) = player.setVolume(volume, volume)

  override fun seekTo(to: Int) = player.seekTo(to)

  override fun isPlaying() = player.isPlaying

  override fun start() = player.start()

  override fun pause() = player.pause()

  override fun setWakeMode(mode: Int) = player.setWakeMode(context, mode)

  override val duration: Int
    get() = player.duration

  override var playbackSpeed: Float = 1F

  override fun prepare(file: File) {
    player.setDataSource(file.absolutePath)
    player.prepare()
  }

  override fun reset() = player.reset()

  override fun setAudioStreamType(streamType: Int) = player.setAudioStreamType(streamType)

  override val currentPosition: Int
    get() = player.currentPosition
}