package de.ph1b.audiobook.koin

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.support.v4.media.session.MediaSessionCompat
import de.ph1b.audiobook.playback.MediaPlayer
import de.ph1b.audiobook.playback.MediaSessionCallback
import de.ph1b.audiobook.playback.NotifyOnAutoConnectionChange
import de.ph1b.audiobook.playback.OnlyAudioRenderersFactory
import de.ph1b.audiobook.playback.PlaybackService
import de.ph1b.audiobook.playback.events.MediaEventReceiver
import de.ph1b.audiobook.playback.utils.ChangeNotifier
import de.ph1b.audiobook.playback.utils.NotificationCreator
import de.ph1b.audiobook.playback.utils.audioFocus.AudioFocusHandler
import org.koin.dsl.module.module

val PlaybackModule = module {
  scope(PLAYBACK_SERVICE_SCOPE) {
    mediaSession(it[0], get())
  }
  scope(PLAYBACK_SERVICE_SCOPE) {
    ChangeNotifier(
      get(),
      get(),
      get(),
      get(),
      get()
    )
  }
  scope(PLAYBACK_SERVICE_SCOPE) {
    NotificationCreator(
      get(),
      get(),
      get(),
      get(),
      get(),
      get()
    )
  }
  scope(PLAYBACK_SERVICE_SCOPE) {
    NotifyOnAutoConnectionChange(get(), get(), currentBookIdPref(), get())
  }
  single {
    MediaPlayer(get(), autoRewindAmountPref(), seekTimePref(), get(), get(), get(), get(), get())
  }
  factory { MediaSessionCallback(get(), currentBookIdPref(), get(), get(), get(), get()) }
  factory {
    OnlyAudioRenderersFactory(get())
  }
  factory {
    AudioFocusHandler(get(), get(), get(), get(), resumeAfterCallPref())
  }
}

private fun mediaSession(
  service: PlaybackService,
  callback: MediaSessionCallback
): MediaSessionCompat {
  val mbrComponentName = ComponentName(service.packageName, MediaEventReceiver::class.java.name)
  val mediaButtonIntent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
    component = mbrComponentName
  }
  val buttonReceiverPendingIntent = PendingIntent.getBroadcast(
    service,
    0,
    mediaButtonIntent,
    PendingIntent.FLAG_UPDATE_CURRENT
  )
  return MediaSessionCompat(
    service,
    PlaybackService::class.java.name,
    mbrComponentName,
    buttonReceiverPendingIntent
  ).apply {
    setCallback(callback)
    setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
  }
}
