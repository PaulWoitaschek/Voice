package de.ph1b.audiobook.playback.player

import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import java.io.File
import java.io.IOException


/**
 * Abstraction of android.media.MediaPlayer.
 *
 * @author Paul Woitaschek
 */
abstract class Player {

  protected val completionSubject: PublishSubject<Unit> = PublishSubject.create<Unit>()!!

  protected val errorSubject: PublishSubject<Unit> = PublishSubject.create<Unit>()!!

  protected val preparedSubject: PublishSubject<Unit> = PublishSubject.create<Unit>()!!

  abstract fun seekTo(to: Int)

  abstract fun isPlaying(): Boolean

  abstract fun start()

  abstract fun pause()

  @Throws(IOException::class)
  abstract fun prepare(file: File)

  abstract fun reset()

  abstract fun setWakeMode(mode: Int)

  abstract val currentPosition: Int

  abstract val duration: Int

  abstract var playbackSpeed: Float

  abstract fun setAudioStreamType(streamType: Int)

  abstract fun setVolume(volume: Float)

  val onError: Observable<Unit> = errorSubject

  val onCompletion: Observable<Unit> = completionSubject

  companion object {
    val AUDIO_SESSION_ID = 9
  }
}