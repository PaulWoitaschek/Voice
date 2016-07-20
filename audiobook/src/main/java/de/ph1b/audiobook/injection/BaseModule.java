/*
 * This file is part of Material Audiobook Player.
 *
 * Material Audiobook Player is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 *
 * Material Audiobook Player is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Material Audiobook Player. If not, see <http://www.gnu.org/licenses/>.
 * /licenses/>.
 */

package de.ph1b.audiobook.injection;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import de.paul_woitaschek.mediaplayer.AndroidPlayer;
import de.paul_woitaschek.mediaplayer.AntennaPlayer;
import de.paul_woitaschek.mediaplayer.MediaPlayer;
import de.ph1b.audiobook.persistence.PrefsManager;
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
   static MediaPlayer providePlayer(MediaPlayerCapabilities capabilities, Context context) {
      if (capabilities.getUseCustomMediaPlayer()) {
         return new AntennaPlayer(context);
      } else {
         return new AndroidPlayer(context);
      }
   }
}
