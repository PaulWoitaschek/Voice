package voice.playback.di

import android.content.Context
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.squareup.anvil.annotations.ContributesTo
import dagger.Module
import dagger.Provides
import voice.playback.player.OnlyAudioRenderersFactory
import voice.playback.session.PlaybackService

@Module
@ContributesTo(PlaybackScope::class)
object PlaybackServiceModule {

  @Provides
  @PlaybackScope
  fun mediaSession(service: PlaybackService): MediaSessionCompat {
    return MediaSessionCompat(service, PlaybackService::class.java.simpleName)
  }

  @Provides
  @PlaybackScope
  fun mediaController(context: Context, mediaSession: MediaSessionCompat): MediaControllerCompat {
    return MediaControllerCompat(context, mediaSession)
  }

  @Provides
  @PlaybackScope
  fun mediaSourceFactory(context: Context): MediaSource.Factory {
    val dataSourceFactory = DefaultDataSource.Factory(context)
    val extractorsFactory = DefaultExtractorsFactory()
      .setConstantBitrateSeekingEnabled(true)
    return ProgressiveMediaSource.Factory(dataSourceFactory, extractorsFactory)
  }

  @Provides
  @PlaybackScope
  fun exoPlayer(
    context: Context,
    onlyAudioRenderersFactory: OnlyAudioRenderersFactory,
    mediaSourceFactory: MediaSource.Factory,
  ): ExoPlayer {
    val audioAttributes = AudioAttributes.Builder()
      .setContentType(C.CONTENT_TYPE_SPEECH)
      .setUsage(C.USAGE_MEDIA)
      .build()
    return ExoPlayer.Builder(context, onlyAudioRenderersFactory, mediaSourceFactory)
      .setAudioAttributes(audioAttributes, true)
      .build()
  }
}
