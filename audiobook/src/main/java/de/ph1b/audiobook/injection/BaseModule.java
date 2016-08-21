package de.ph1b.audiobook.injection;

import android.content.Context;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import de.ph1b.audiobook.playback.player.AndroidPlayer;
import de.ph1b.audiobook.playback.player.AntennaPlayer;
import de.ph1b.audiobook.playback.player.Player;
import de.ph1b.audiobook.playback.utils.MediaPlayerCapabilities;

/**
 * Basic providing module.
 *
 * @author Paul Woitaschek
 */
@Module public final class BaseModule {

   @Provides
   @Singleton
   static Player providePlayer(MediaPlayerCapabilities capabilities, Context context) {
      if (capabilities.getUseCustomMediaPlayer()) {
         return new AntennaPlayer(context);
      } else {
         return new AndroidPlayer(context);
      }
   }
}
