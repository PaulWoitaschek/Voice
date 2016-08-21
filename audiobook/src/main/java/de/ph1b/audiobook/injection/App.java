package de.ph1b.audiobook.injection;

import android.app.Application;
import android.content.Intent;
import android.support.v7.app.AppCompatDelegate;

import org.acra.ACRA;
import org.acra.annotation.ReportsCrashes;
import org.acra.config.ACRAConfigurationException;
import org.acra.config.ConfigurationBuilder;
import org.acra.sender.HttpSender;

import javax.inject.Inject;

import de.ph1b.audiobook.BuildConfig;
import de.ph1b.audiobook.features.BookAdder;
import de.ph1b.audiobook.persistence.PrefsManager;
import de.ph1b.audiobook.playback.BookReaderService;
import timber.log.Timber;

@ReportsCrashes(
      httpMethod = HttpSender.Method.PUT,
      reportType = HttpSender.Type.JSON,
      buildConfigClass = BuildConfig.class,
      formUri = "http://acra-63e870.smileupps.com/acra-material/_design/acra-storage/_update/report",
      formUriBasicAuthLogin = "122user",
      formUriBasicAuthPassword = "J7aenghqfQhIcuZn")
public class App extends Application {

   private static ApplicationComponent applicationComponent;
   @Inject BookAdder bookAdder;
   @Inject PrefsManager prefsManager;

   public static ApplicationComponent component() {
      return applicationComponent;
   }

   @Override public void onCreate() {
      super.onCreate();

      // init acra + return early if this is the sender service
      try {
         if (!BuildConfig.DEBUG) {
            ACRA.init(this, new ConfigurationBuilder(this)
                  .build());
         }
      } catch (ACRAConfigurationException e) {
         throw new RuntimeException(e);
      }
      if (ACRA.isACRASenderServiceProcess()) {
         return;
      }

      applicationComponent = newComponent();
      component().inject(this);

      if (BuildConfig.DEBUG) {
         // init timber
         Timber.plant(new Timber.DebugTree());

         // force enable acra in debug mode
         prefsManager.setAcraEnabled(true);
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