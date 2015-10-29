package de.ph1b.audiobook.utils;

import android.app.Application;
import android.content.Context;
import android.os.Build;

import java.util.Arrays;
import java.util.List;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import de.ph1b.audiobook.mediaplayer.AndroidMediaPlayer;
import de.ph1b.audiobook.mediaplayer.CustomMediaPlayer;
import de.ph1b.audiobook.mediaplayer.MediaPlayerInterface;

/**
 * Basic providing module.
 *
 * @author Paul Woitaschek
 */
@Module
public class BaseModule {

    private final Context appContext;

    public BaseModule(Application application) {
        this.appContext = application;
    }

    /**
     * Checks if the device can set playback-seed by {@link MediaPlayerInterface#setPlaybackSpeed(float)}
     * Therefore it has to be >= {@link android.os.Build.VERSION_CODES#JELLY_BEAN} and not blacklisted
     * due to a bug.
     *
     * @return true if the device can set variable playback speed.
     */
    public static boolean canSetSpeed() {
        boolean greaterJellyBean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;
        List<String> hwBlacklist = Arrays.asList("mt6572", "mt6575", "mt6582", "mt6589", "mt6592",
                "mt8125");
        return greaterJellyBean && !(hwBlacklist.contains(Build.HARDWARE));
    }

    @Singleton
    @Provides
    Context provideAppContext() {
        return appContext;
    }

    @Provides
    MediaPlayerInterface provideMediaPlayer() {
        if (canSetSpeed()) {
            return new CustomMediaPlayer();
        } else {
            return new AndroidMediaPlayer();
        }
    }
}
