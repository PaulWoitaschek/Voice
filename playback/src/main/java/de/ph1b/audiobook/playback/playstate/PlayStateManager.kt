package de.ph1b.audiobook.playback.playstate

import android.support.v4.media.session.PlaybackStateCompat
import io.reactivex.BackpressureStrategy
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the playback state and is able to inform subscriber.
 * Also manages the reason for pausing and sets it to none if the state gets stopped is playing.
 */
@Singleton
class PlayStateManager
@Inject
constructor() {

  private val playStateSubject = BehaviorSubject.createDefault(PlayState.Stopped)

  init {
    @Suppress("CheckResult")
    playStateSubject.subscribe {
      if (it == PlayState.Playing || it == PlayState.Stopped) {
        pauseReason = PauseReason.NONE
      }
    }
  }

  var pauseReason = PauseReason.NONE

  fun playStateStream(): Observable<PlayState> = playStateSubject.distinctUntilChanged()
  fun playStateFlow(): Flow<PlayState> = playStateSubject.toFlowable(BackpressureStrategy.LATEST).asFlow()

  var playState: PlayState
    set(value) {
      if (playStateSubject.value != value) {
        Timber.i("playState set to $value")
        playStateSubject.onNext(value)
      }
    }
    get() = playStateSubject.value!!

  /** Represents the play states for the playback.  */
  enum class PlayState(@PlaybackStateCompat.State val playbackStateCompat: Int) {
    Playing(PlaybackStateCompat.STATE_PLAYING),
    Paused(PlaybackStateCompat.STATE_PAUSED),
    Stopped(PlaybackStateCompat.STATE_STOPPED)
  }

  enum class PauseReason {
    NONE,
    CALL,
    BECAUSE_HEADSET,
    LOSS_TRANSIENT
  }
}
