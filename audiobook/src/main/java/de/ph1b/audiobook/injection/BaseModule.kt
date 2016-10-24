package de.ph1b.audiobook.injection

import android.content.Context
import com.tbruyelle.rxpermissions.RxPermissions
import dagger.Module
import dagger.Provides
import dagger.Reusable
import de.ph1b.audiobook.playback.player.AndroidPlayer
import de.ph1b.audiobook.playback.player.AntennaPlayer
import de.ph1b.audiobook.playback.player.Player
import de.ph1b.audiobook.playback.utils.MediaPlayerCapabilities
import javax.inject.Singleton

/**
 * Basic providing module.

 * @author Paul Woitaschek
 */
@Module class BaseModule {

    @Provides @Singleton fun providePlayer(capabilities: MediaPlayerCapabilities, context: Context): Player {
        if (capabilities.useCustomMediaPlayer()) {
            return AntennaPlayer(context)
        } else {
            return AndroidPlayer(context)
        }
    }

    @Provides @Reusable fun provideRxPermissions(context: Context): RxPermissions = RxPermissions.getInstance(context)
}
