package de.ph1b.audiobook.playback

import android.support.v4.media.session.PlaybackStateCompat
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
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

  private val playStateSubject = BehaviorSubject.createDefault(PlayState.STOPPED)

  init {
    playStateSubject.subscribe {
      if (it == PlayState.PLAYING || it == PlayState.STOPPED) {
        pauseReason = PauseReason.NONE
      }
    }
  }

  var pauseReason = PauseReason.NONE

  fun playStateStream(): Observable<PlayState> = playStateSubject.distinctUntilChanged()

  var playState: PlayState
    set(value) {
      Timber.i("playState set to $value")
      playStateSubject.onNext(value)
    }
    get() = playStateSubject.value!!

  /** Represents the play states for the playback.  */
  enum class PlayState(@PlaybackStateCompat.State val playbackStateCompat: Int) {

    PLAYING(PlaybackStateCompat.STATE_PLAYING),
    PAUSED(PlaybackStateCompat.STATE_PAUSED),
    STOPPED(PlaybackStateCompat.STATE_STOPPED)
  }

  enum class PauseReason {
    NONE,
    CALL,
    BECAUSE_HEADSET,
    LOSS_TRANSIENT
  }
}
