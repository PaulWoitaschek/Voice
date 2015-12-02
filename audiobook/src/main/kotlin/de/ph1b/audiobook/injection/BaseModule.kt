package de.ph1b.audiobook.injection

import android.content.Context
import android.os.Build
import dagger.Module
import dagger.Provides
import de.ph1b.audiobook.mediaplayer.AndroidMediaPlayer
import de.ph1b.audiobook.mediaplayer.AntennaPlayer
import de.ph1b.audiobook.mediaplayer.MediaPlayerInterface
import de.ph1b.audiobook.uitools.ImageLinkService
import retrofit.GsonConverterFactory
import retrofit.Retrofit
import retrofit.RxJavaCallAdapterFactory
import java.util.*
import javax.inject.Singleton

/**
 * Basic providing module.

 * @author Paul Woitaschek
 */
@Module
class BaseModule {

    @Provides
    fun provideMediaPlayer(context: Context): MediaPlayerInterface {
        if (MIN_MARSHMALLOW || !canUseSonic()) {
            return AndroidMediaPlayer()
        } else {
            return AntennaPlayer(context)
        }
    }

    @Provides
    @Singleton
    fun provideImageLinkService(): ImageLinkService {
        val retrofit = Retrofit.Builder().baseUrl("https://ajax.googleapis.com/ajax/services/search/").addConverterFactory(GsonConverterFactory.create()).addCallAdapterFactory(RxJavaCallAdapterFactory.create()).build()

        return retrofit.create(ImageLinkService::class.java)
    }

    companion object {

        private val MIN_MARSHMALLOW = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
        private val MIN_JELLYBEAN = Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN

        /**
         * Checks if the device can set playback-seed by [MediaPlayerInterface.setPlaybackSpeed]
         * Therefore it has to be >= [android.os.Build.VERSION_CODES.JELLY_BEAN] and not blacklisted
         * due to a bug.

         * @return true if the device can set variable playback speed.
         */
        fun canSetSpeed(): Boolean {
            return MIN_MARSHMALLOW || canUseSonic()
        }

        private fun canUseSonic(): Boolean {
            val hwBlacklist = Arrays.asList("mt6572", "mt6575", "mt6582", "mt6589", "mt6592",
                    "mt8125")
            return MIN_JELLYBEAN && !hwBlacklist.contains(Build.HARDWARE.toLowerCase())
        }
    }
}
