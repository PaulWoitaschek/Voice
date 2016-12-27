package de.ph1b.audiobook.playback.utils

import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Timeline
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.TrackSelectionArray

/**
 * Extension that just provides default methods for better readability
 *
 * @author Paul Woitaschek
 */
interface SimpleEventListener : ExoPlayer.EventListener {
  override fun onTracksChanged(trackGroups: TrackGroupArray?, trackSelections: TrackSelectionArray?) {

  }

  override fun onPlayerError(error: ExoPlaybackException) {
  }

  override fun onLoadingChanged(isLoading: Boolean) {
  }

  override fun onPositionDiscontinuity() {
  }

  override fun onTimelineChanged(timeline: Timeline?, manifest: Any?) {
  }

  override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
  }
}