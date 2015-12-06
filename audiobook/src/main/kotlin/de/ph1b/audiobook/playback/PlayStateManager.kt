package de.ph1b.audiobook.playback

import android.support.v4.media.session.PlaybackStateCompat
import rx.subjects.BehaviorSubject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the playback state and is able to inform subscriber.
 *
 * @author Paul Woitaschek
 */
@Singleton
class PlayStateManager
@Inject
constructor() {

    val playState = BehaviorSubject.create(PlayStateManager.PlayState.STOPPED)

    /**
     * Represents the play states for the playback.
     *
     * @author Paul Woitaschek
     */
    enum class PlayState internal constructor(@PlaybackStateCompat.State public val playbackStateCompat: Int) {
        PLAYING(PlaybackStateCompat.STATE_PLAYING),
        PAUSED(PlaybackStateCompat.STATE_PAUSED),
        STOPPED(PlaybackStateCompat.STATE_STOPPED)
    }
}