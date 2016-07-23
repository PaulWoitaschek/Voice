package de.ph1b.audiobook.injection;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import de.ph1b.audiobook.persistence.PrefsManager;
import de.ph1b.audiobook.playback.player.AndroidPlayer;
import de.ph1b.audiobook.playback.player.AntennaPlayer;
import de.ph1b.audiobook.playback.player.Player;
import de.ph1b.audiobook.playback.utils.MediaPlayerCapabilities;

/**
 * Basic providing module.
 *
 * @author Paul Woitaschek
 */
@Module
public class BaseModule {

   @Provides
   @Named(PrefsManager.FOR)
   static SharedPreferences providePrefsForManager(Context context) {
      return PreferenceManager.getDefaultSharedPreferences(context);
   }

   @Provides
   @Named(MediaPlayerCapabilities.FOR)
   static SharedPreferences provideForMediaPlayerCapabilities(Context context) {
      String name = "forMediaPlayerCapabilities";
      return context.getSharedPreferences(name, Context.MODE_PRIVATE);
   }

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
