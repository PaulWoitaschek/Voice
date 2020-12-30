package de.ph1b.audiobook.injection

import android.content.Context
import com.google.android.exoplayer2.SimpleExoPlayer
import com.squareup.anvil.annotations.ContributesTo
import dagger.Module
import dagger.Provides
import de.ph1b.audiobook.AppScope
import de.ph1b.audiobook.playback.OnlyAudioRenderersFactory

@Module
@ContributesTo(AppScope::class)
object PlaybackModule {

  @Provides
  @JvmStatic
  fun exoPlayer(context: Context, onlyAudioRenderersFactory: OnlyAudioRenderersFactory): SimpleExoPlayer {
    return SimpleExoPlayer.Builder(context, onlyAudioRenderersFactory)
      .build()
  }
}
