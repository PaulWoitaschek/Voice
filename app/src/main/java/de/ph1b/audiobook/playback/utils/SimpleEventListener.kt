package de.ph1b.audiobook.playback.utils

import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Timeline
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.TrackSelectionArray

/**
 * Empty default callbacks for better readability
 */
interface SimpleEventListener : Player.EventListener {

  override fun onTracksChanged(trackGroups: TrackGroupArray?, trackSelections: TrackSelectionArray?) {}
  override fun onPlayerError(error: ExoPlaybackException) {}
  override fun onLoadingChanged(isLoading: Boolean) {}
  override fun onPositionDiscontinuity() {}
  override fun onTimelineChanged(timeline: Timeline?, manifest: Any?) {}
  override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {}
  override fun onPlaybackParametersChanged(p0: PlaybackParameters?) {}
  override fun onRepeatModeChanged(p0: Int) {}
}
