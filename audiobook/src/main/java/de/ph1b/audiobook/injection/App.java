package de.ph1b.audiobook.injection;

import android.app.Application;
import android.content.Intent;
import android.support.v7.app.AppCompatDelegate;

import org.acra.ACRA;
import org.acra.annotation.ReportsCrashes;
import org.acra.config.ACRAConfiguration;
import org.acra.config.ACRAConfigurationException;
import org.acra.config.ConfigurationBuilder;
import org.acra.sender.HttpSender.Method;
import org.acra.sender.HttpSender.Type;

import java.util.Random;

import javax.inject.Inject;

import de.ph1b.audiobook.BuildConfig;
import de.ph1b.audiobook.features.BookAdder;
import de.ph1b.audiobook.persistence.PrefsManager;
import de.ph1b.audiobook.playback.PlaybackService;
import timber.log.Timber;

@ReportsCrashes(
  httpMethod = Method.PUT,
  reportType = Type.JSON,
  buildConfigClass = BuildConfig.class,
  formUri = "http://acra-f85814.smileupps.com/acra-myapp-0b5541/_design/acra-storage/_update/report",
  formUriBasicAuthLogin = "129user",
  formUriBasicAuthPassword = "IQykOJBswx7C7YtY")
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
    if (!BuildConfig.DEBUG) {
      boolean isSenderProcess = ACRA.isACRASenderServiceProcess();
      if (isSenderProcess || new Random().nextInt(5) == 0)
        try {
          ACRAConfiguration config = new ConfigurationBuilder(this)
            .build();
          ACRA.init(this, config);
        } catch (ACRAConfigurationException e) {
          throw new RuntimeException(e);
        }

      if (isSenderProcess) return;
    }

    applicationComponent = newComponent();
    component().inject(this);

    if (BuildConfig.DEBUG) {
      // init timber
      Timber.plant(new Timber.DebugTree());
    }

    Timber.i("onCreate");

    bookAdder.scanForFiles(true);
    startService(new Intent(this, PlaybackService.class));

    //noinspection WrongConstant,ConstantConditions
    AppCompatDelegate.setDefaultNightMode(prefsManager.getTheme().get().getNightMode());
  }

  protected ApplicationComponent newComponent() {
    return DaggerApplicationComponent.builder()
      .androidModule(new AndroidModule(this))
      .build();
  }
}