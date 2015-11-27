package de.ph1b.audiobook.injection;

import android.app.ActivityManager;
import android.app.Application;
import android.app.NotificationManager;
import android.content.Context;
import android.media.AudioManager;
import android.net.ConnectivityManager;
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
@Module
public class AndroidModule {

    private final Context context;

    public AndroidModule(Application application) {
        context = application;
    }

    @Provides
    Context provideContext() {
        return context;
    }

    @Provides
    @Singleton
    AudioManager provideAudioManager(Context context) {
        return (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    @Provides
    @Singleton
    ActivityManager provideActivityManager(Context context) {
        return (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
    }

    @Provides
    @Singleton
    TelephonyManager provideTelephonyManager(Context context) {
        return (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
    }

    @Provides
    @Singleton
    ConnectivityManager provideConnectivityManager(Context context) {
        return (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    @Provides
    @Singleton
    WindowManager provideWindowManager(Context context) {
        return (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    }

    @Provides
    @Singleton
    NotificationManager provideNotificationManager(Context context) {
        return (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }
}
