package voice.playback.di

import android.content.Context
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.extractor.DefaultExtractorsFactory
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
      .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
      .setUsage(C.USAGE_MEDIA)
      .build()
    return ExoPlayer.Builder(context, onlyAudioRenderersFactory, mediaSourceFactory)
      .setAudioAttributes(audioAttributes, true)
      .build()
  }
}
