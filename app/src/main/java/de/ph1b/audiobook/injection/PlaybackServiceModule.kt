package de.ph1b.audiobook.injection

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.support.v4.media.session.MediaSessionCompat
import dagger.Module
import dagger.Provides
import de.ph1b.audiobook.playback.MediaSessionCallback
import de.ph1b.audiobook.playback.PlaybackService
import de.ph1b.audiobook.playback.events.MediaEventReceiver

@Module
object PlaybackServiceModule {

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
    }
  }
}
