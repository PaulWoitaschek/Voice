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
import dagger.Module
import dagger.Provides
import de.ph1b.audiobook.common.ApplicationIdProvider
import de.ph1b.audiobook.covercolorextractor.CoverColorExtractor
import de.ph1b.audiobook.misc.ApplicationIdProviderImpl
import de.ph1b.audiobook.misc.ToBookIntentProviderImpl
import de.ph1b.audiobook.playback.notification.ToBookIntentProvider
import javax.inject.Singleton

/**
 * Module providing Android SDK Related instances.
 */
@Module
object AndroidModule {

  @Provides
  @JvmStatic
  fun provideContext(app: Application): Context = app

  @Provides
  @Singleton
  @JvmStatic
  fun provideAudioManager(context: Context) =
    context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

  @Provides
  @Singleton
  @JvmStatic
  fun provideActivityManager(context: Context) =
    context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

  @Provides
  @Singleton
  @JvmStatic
  fun provideTelephonyManager(context: Context) =
    context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

  @Provides
  @Singleton
  @JvmStatic
  fun provideConnectivityManager(context: Context) =
    context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

  @Provides
  @JvmStatic
  fun provideWindowManager(context: Context) =
    context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

  @Provides
  @Singleton
  @JvmStatic
  fun provideNotificationManager(context: Context) =
    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

  @Provides
  @Singleton
  @JvmStatic
  fun provideSensorManager(context: Context) =
    context.getSystemService(Context.SENSOR_SERVICE) as SensorManager?

  @Provides
  @Singleton
  @JvmStatic
  fun providePowerManager(context: Context) =
    context.getSystemService(Context.POWER_SERVICE) as PowerManager

  @Provides
  @Singleton
  @JvmStatic
  fun provideCoverColorExtractor(): CoverColorExtractor {
    return CoverColorExtractor()
  }

  @Provides
  fun toToBookIntentProvider(impl: ToBookIntentProviderImpl): ToBookIntentProvider = impl

  @Provides
  fun applicationIdProvider(impl: ApplicationIdProviderImpl): ApplicationIdProvider = impl
}
