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
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import dagger.Module
import dagger.Provides
import javax.inject.Singleton


/**
 * Module providing Android SDK Related instances.
 *
 * @author Paul Woitaschek
 */
@Module class AndroidModule constructor(private val application: Application) {
  @Provides fun provideContext(): Context = application
  @Provides @Singleton fun provideAudioManager(context: Context) = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
  @Provides @Singleton fun provideActivityManager(context: Context) = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
  @Provides @Singleton fun provideTelephonyManager(context: Context) = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
  @Provides @Singleton fun provideConnectivityManager(context: Context) = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
  @Provides @Singleton fun provideWindowManager(context: Context) = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
  @Provides @Singleton fun provideNotificationManager(context: Context) = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
  @Provides @Singleton fun provideSensorManager(context: Context) = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager?
  @Provides @Singleton fun providePowerManager(context: Context) = context.getSystemService(Context.POWER_SERVICE) as PowerManager

  @Provides @Singleton fun provideExoPlayer(context: Context): SimpleExoPlayer {
    val trackSelector = DefaultTrackSelector()
    val loadControl = DefaultLoadControl()
    return ExoPlayerFactory.newSimpleInstance(context, trackSelector, loadControl)
  }

  @Provides @Singleton fun provideDataSourceFactory(context: Context): DataSource.Factory {
    val userAgent = Util.getUserAgent(context, context.packageName)
    return DefaultDataSourceFactory(context, userAgent, null)
  }
}
