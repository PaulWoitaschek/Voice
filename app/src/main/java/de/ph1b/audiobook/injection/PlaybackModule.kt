package de.ph1b.audiobook.injection

import android.content.Context
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import dagger.Module
import dagger.Provides
import de.ph1b.audiobook.playback.OnlyAudioRenderersFactory

@Module
object PlaybackModule {

  @Provides
  @JvmStatic
  fun exoPlayer(context: Context, onlyAudioRenderersFactory: OnlyAudioRenderersFactory): SimpleExoPlayer {
    return ExoPlayerFactory.newSimpleInstance(context, onlyAudioRenderersFactory, DefaultTrackSelector())
  }
}
