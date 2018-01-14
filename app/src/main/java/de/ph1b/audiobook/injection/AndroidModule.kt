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
import com.squareup.moshi.Moshi
import com.squareup.picasso.Picasso
import dagger.Module
import dagger.Provides
import de.paulwoitaschek.chapterreader.ChapterReader
import de.paulwoitaschek.chapterreader.ChapterReaderFactory
import de.ph1b.audiobook.covercolorextractor.CoverColorExtractor
import de.ph1b.audiobook.features.crashlytics.CrashlyticsProxy
import de.ph1b.audiobook.misc.ErrorReporter
import javax.inject.Singleton

/**
 * Module providing Android SDK Related instances.
 */
@Module
class AndroidModule {

  @Provides
  fun provideContext(app: Application): Context = app

  @Provides
  @Singleton
  fun provideAudioManager(context: Context) = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

  @Provides
  @Singleton
  fun provideActivityManager(context: Context) = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

  @Provides
  @Singleton
  fun provideTelephonyManager(context: Context) = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

  @Provides
  @Singleton
  fun provideConnectivityManager(context: Context) = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

  @Provides
  fun provideWindowManager(context: Context) = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

  @Provides
  @Singleton
  fun provideNotificationManager(context: Context) = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

  @Provides
  @Singleton
  fun provideSensorManager(context: Context) = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager?

  @Provides
  @Singleton
  fun providePowerManager(context: Context) = context.getSystemService(Context.POWER_SERVICE) as PowerManager

  @Provides
  @Singleton
  fun provideMoshi(): Moshi = Moshi.Builder().build()

  @Provides
  @Singleton
  fun provideErrorReporter(): ErrorReporter = CrashlyticsProxy

  @Provides
  @Singleton
  fun provideChapterReader(): ChapterReader = ChapterReaderFactory.create()

  @Provides
  @Singleton
  fun provideCoverColorExtractor(context: Context): CoverColorExtractor {
    val picasso = Picasso.with(context)
    return CoverColorExtractor(picasso)
  }
}
