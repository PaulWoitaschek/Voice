package de.ph1b.audiobook.injection;

import android.content.Context;
import android.os.Build;

import java.util.Arrays;
import java.util.List;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import de.ph1b.audiobook.mediaplayer.AndroidMediaPlayer;
import de.ph1b.audiobook.mediaplayer.AntennaPlayer;
import de.ph1b.audiobook.mediaplayer.MediaPlayerInterface;
import de.ph1b.audiobook.uitools.ImageLinkService;
import retrofit.GsonConverterFactory;
import retrofit.Retrofit;
import retrofit.RxJavaCallAdapterFactory;

/**
 * Basic providing module.
 *
 * @author Paul Woitaschek
 */
@Module
public class BaseModule {

    private static final boolean MIN_MARSHMALLOW = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    private static final boolean MIN_JELLYBEAN = Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;

    /**
     * Checks if the device can set playback-seed by {@link MediaPlayerInterface#setPlaybackSpeed(float)}
     * Therefore it has to be >= {@link android.os.Build.VERSION_CODES#JELLY_BEAN} and not blacklisted
     * due to a bug.
     *
     * @return true if the device can set variable playback speed.
     */
    public static boolean canSetSpeed() {
        return MIN_MARSHMALLOW || canUseSonic();
    }

    private static boolean canUseSonic() {
        List<String> hwBlacklist = Arrays.asList("mt6572", "mt6575", "mt6582", "mt6589", "mt6592",
                "mt8125");
        return MIN_JELLYBEAN && !hwBlacklist.contains(Build.HARDWARE.toLowerCase());
    }

    @Provides
    MediaPlayerInterface provideMediaPlayer(Context context) {
        if (MIN_MARSHMALLOW || !canUseSonic()) {
            return new AndroidMediaPlayer();
        } else {
            return new AntennaPlayer(context);
        }
    }

    @Provides
    @Singleton
    ImageLinkService provideImageLinkService() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://ajax.googleapis.com/ajax/services/search/")
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .build();

        return retrofit.create(ImageLinkService.class);
    }
}
