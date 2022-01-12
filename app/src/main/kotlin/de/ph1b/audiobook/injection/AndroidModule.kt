package de.ph1b.audiobook.injection

import android.app.ActivityManager
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.hardware.SensorManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.os.PowerManager
import android.telephony.TelephonyManager
import android.view.WindowManager
import com.squareup.anvil.annotations.ContributesTo
import dagger.Module
import dagger.Provides
import de.ph1b.audiobook.AppScope
import de.ph1b.audiobook.common.ApplicationIdProvider
import de.ph1b.audiobook.misc.ApplicationIdProviderImpl
import de.ph1b.audiobook.misc.ToBookIntentProviderImpl
import de.ph1b.audiobook.playback.notification.ToBookIntentProvider
import kotlinx.serialization.json.Json
import javax.inject.Singleton

/**
 * Module providing Android SDK Related instances.
 */
@Module
@ContributesTo(AppScope::class)
object AndroidModule {

  @Provides
  fun provideContext(app: Application): Context = app

  @Provides
  @Singleton
  fun provideAudioManager(context: Context): AudioManager {
    return context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
  }

  @Provides
  @Singleton
  fun provideActivityManager(context: Context): ActivityManager {
    return context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
  }

  @Provides
  @Singleton
  fun provideTelephonyManager(context: Context): TelephonyManager {
    return context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
  }

  @Provides
  @Singleton
  fun provideConnectivityManager(context: Context): ConnectivityManager {
    return context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
  }

  @Provides
  fun provideWindowManager(context: Context): WindowManager {
    return context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
  }

  @Provides
  @Singleton
  fun provideNotificationManager(context: Context): NotificationManager {
    return context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
  }

  @Provides
  @Singleton
  fun provideSensorManager(context: Context): SensorManager? {
    return context.getSystemService(Context.SENSOR_SERVICE) as SensorManager?
  }

  @Provides
  @Singleton
  fun providePowerManager(context: Context): PowerManager {
    return context.getSystemService(Context.POWER_SERVICE) as PowerManager
  }

  @Provides
  fun toToBookIntentProvider(impl: ToBookIntentProviderImpl): ToBookIntentProvider = impl

  @Provides
  fun applicationIdProvider(impl: ApplicationIdProviderImpl): ApplicationIdProvider = impl

  @Provides
  @Singleton
  fun json(): Json {
    return Json.Default
  }
}
