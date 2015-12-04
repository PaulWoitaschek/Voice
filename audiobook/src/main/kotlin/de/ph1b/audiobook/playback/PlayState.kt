package de.ph1b.audiobook.playback

import android.support.v4.media.session.PlaybackStateCompat

/**
 * Represents the play states for the audiobook playback.
 *
 * @author Paul Woitaschek
 */
enum class PlayState internal constructor(@PlaybackStateCompat.State public val playbackStateCompat: Int) {
    PLAYING(PlaybackStateCompat.STATE_PLAYING),
    PAUSED(PlaybackStateCompat.STATE_PAUSED),
    STOPPED(PlaybackStateCompat.STATE_STOPPED);
}