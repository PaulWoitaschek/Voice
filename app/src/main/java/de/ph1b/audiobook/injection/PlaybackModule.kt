package de.ph1b.audiobook.injection

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.support.v4.media.session.MediaSessionCompat
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import dagger.Module
import dagger.Provides
import de.ph1b.audiobook.playback.MediaSessionCallback
import de.ph1b.audiobook.playback.OnlyAudioRenderersFactory
import de.ph1b.audiobook.playback.PlaybackService
import de.ph1b.audiobook.playback.events.MediaEventReceiver

/**
 * Module for playback related classes
 */
@Module
object PlaybackModule {

  @Provides
  @JvmStatic
  fun provideMediaButtonReceiverComponentName(service: PlaybackService): ComponentName {
    return ComponentName(service.packageName, MediaEventReceiver::class.java.name)
  }

  @Provides
  @PerService
  @JvmStatic
  fun provideButtonRecieverPendingIntent(
    service: PlaybackService,
    mbrComponentName: ComponentName
  ): PendingIntent {
    val mediaButtonIntent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
      component = mbrComponentName
    }
    return PendingIntent.getBroadcast(
      service,
      0,
      mediaButtonIntent,
      PendingIntent.FLAG_UPDATE_CURRENT
    )
  }

  @Provides
  @PerService
  @JvmStatic
  fun provideMediaSession(
    service: PlaybackService,
    callback: MediaSessionCallback,
    mbrComponentName: ComponentName,
    buttonReceiverPendingIntent: PendingIntent
  ): MediaSessionCompat {
    return MediaSessionCompat(
      service,
      PlaybackService::class.java.name,
      mbrComponentName,
      buttonReceiverPendingIntent
    ).apply {
      setCallback(callback)
      setFlags(
        MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS
            or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
      )
    }
  }

  @Provides
  @JvmStatic
  fun exoPlayer(context: Context, onlyAudioRenderersFactory: OnlyAudioRenderersFactory): SimpleExoPlayer {
    return ExoPlayerFactory.newSimpleInstance(context, onlyAudioRenderersFactory, DefaultTrackSelector())
  }
}
