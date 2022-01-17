package de.ph1b.audiobook.playback.di

import android.content.Context
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.audio.AudioAttributes
import com.squareup.anvil.annotations.ContributesTo
import dagger.Module
import dagger.Provides
import de.ph1b.audiobook.playback.player.OnlyAudioRenderersFactory
import de.ph1b.audiobook.playback.session.PlaybackService

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
  fun exoPlayer(context: Context, onlyAudioRenderersFactory: OnlyAudioRenderersFactory): ExoPlayer {
    val audioAttributes = AudioAttributes.Builder()
      .setContentType(C.CONTENT_TYPE_SPEECH)
      .setUsage(C.USAGE_MEDIA)
      .build()
    return ExoPlayer.Builder(context, onlyAudioRenderersFactory)
      .setAudioAttributes(audioAttributes, true)
      .build()
  }
}
