package de.ph1b.audiobook.playback.di

import android.content.Context
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import dagger.Module
import dagger.Provides
import de.ph1b.audiobook.playback.session.PlaybackService

@Module
object PlaybackServiceModule {

  @Provides
  @PerService
  fun mediaSession(service: PlaybackService): MediaSessionCompat {
    return MediaSessionCompat(service, PlaybackService::class.java.simpleName)
  }

  @Provides
  @PerService
  fun mediaController(context: Context, mediaSession: MediaSessionCompat): MediaControllerCompat {
    return MediaControllerCompat(context, mediaSession)
  }
}
