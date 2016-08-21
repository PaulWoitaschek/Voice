package de.ph1b.audiobook.injection;

import android.app.Application;
import android.content.Intent;
import android.support.v7.app.AppCompatDelegate;

import javax.inject.Inject;

import de.ph1b.audiobook.BuildConfig;
import de.ph1b.audiobook.features.BookAdder;
import de.ph1b.audiobook.persistence.PrefsManager;
import de.ph1b.audiobook.playback.BookReaderService;
import timber.log.Timber;

public class App extends Application {

   private static ApplicationComponent applicationComponent;

   @Inject BookAdder bookAdder;
   @Inject PrefsManager prefsManager;

   public static ApplicationComponent component() {
      return applicationComponent;
   }

   @Override public void onCreate() {
      super.onCreate();

      applicationComponent = newComponent();
      component().inject(this);

      if (BuildConfig.DEBUG) {
         // init timber
         Timber.plant(new Timber.DebugTree());
      }

      Timber.i("onCreate");

      bookAdder.scanForFiles(true);
      startService(new Intent(this, BookReaderService.class));

      //noinspection WrongConstant
      AppCompatDelegate.setDefaultNightMode(prefsManager.getTheme().getNightMode());
   }

   protected ApplicationComponent newComponent() {
      return DaggerApplicationComponent.builder()
            .androidModule(new AndroidModule(this))
            .build();
   }
}