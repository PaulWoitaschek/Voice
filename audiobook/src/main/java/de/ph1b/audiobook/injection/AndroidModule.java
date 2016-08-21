package de.ph1b.audiobook.injection;

import android.app.ActivityManager;
import android.app.Application;
import android.app.NotificationManager;
import android.content.Context;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.support.annotation.Nullable;
import android.telephony.TelephonyManager;
import android.view.WindowManager;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * Module providing Android SDK Related instances.
 *
 * @author Paul Woitaschek
 */
@Module public class AndroidModule {

   private final Context context;

   AndroidModule(Application application) {
      context = application;
   }

   @Provides @Singleton
   static AudioManager provideAudioManager(Context context) {
      return (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
   }

   @Provides @Singleton
   static ActivityManager provideActivityManager(Context context) {
      return (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
   }

   @Provides @Singleton
   static TelephonyManager provideTelephonyManager(Context context) {
      return (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
   }

   @Provides @Singleton
   static ConnectivityManager provideConnectivityManager(Context context) {
      return (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
   }

   @Provides @Singleton
   static WindowManager provideWindowManager(Context context) {
      return (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
   }

   @Provides @Singleton
   static NotificationManager provideNotificationManager(Context context) {
      return (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
   }

   @Provides @Singleton
   @Nullable static SensorManager provideSensorManager(Context context) {
      return (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
   }

   @Provides
   Context provideContext() {
      return context;
   }
}
