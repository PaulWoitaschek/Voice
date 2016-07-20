package de.ph1b.audiobook.playback

import android.support.v4.media.session.PlaybackStateCompat
import rx.subjects.BehaviorSubject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the playback state and is able to inform subscriber.
 * Also manages the reason for pausing and sets it to none if the state gets stopped is playing.
 *
 * @author Paul Woitaschek
 */
@Singleton
class PlayStateManager
@Inject
constructor() {

    val playState: BehaviorSubject<PlayState> = BehaviorSubject.create(PlayStateManager.PlayState.STOPPED)

    init {
        playState.subscribe {
            if (it == PlayState.PLAYING || it == PlayState.STOPPED) {
                pauseReason = PauseReason.NONE
            }
        }
    }

    var pauseReason = PauseReason.NONE

    /**
     * Represents the play states for the playback.
     *
     * @author Paul Woitaschek
     */
    enum class PlayState(@PlaybackStateCompat.State val playbackStateCompat: Int) {
        PLAYING(PlaybackStateCompat.STATE_PLAYING),
        PAUSED(PlaybackStateCompat.STATE_PAUSED),
        STOPPED(PlaybackStateCompat.STATE_STOPPED)
    }

    enum class PauseReason {
        NONE,
        BECAUSE_HEADSET,
        LOSS_TRANSIENT
    }
}