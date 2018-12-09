package de.ph1b.audiobook.injection

import android.app.Application
import android.os.Looper
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatDelegate
import com.squareup.picasso.Picasso
import de.ph1b.audiobook.BuildConfig
import de.ph1b.audiobook.data.di.DataInjector
import de.ph1b.audiobook.features.BookAdder
import de.ph1b.audiobook.features.crashlytics.CrashLoggingTree
import de.ph1b.audiobook.features.crashlytics.CrashlyticsProxy
import de.ph1b.audiobook.features.widget.TriggerWidgetOnChange
import de.ph1b.audiobook.misc.StrictModeInit
import de.ph1b.audiobook.persistence.pref.Pref
import de.ph1b.audiobook.playback.AndroidAutoConnectedReceiver
import de.ph1b.audiobook.uitools.ThemeUtil
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

class App : Application() {

  @Inject
  lateinit var bookAdder: BookAdder
  @Inject
  lateinit var triggerWidgetOnChange: TriggerWidgetOnChange
  @Inject
  lateinit var autoConnectedReceiver: AndroidAutoConnectedReceiver
  @field:[Inject Named(PrefKeys.THEME)]
  lateinit var themePref: Pref<ThemeUtil.Theme>

  override fun onCreate() {
    super.onCreate()

    if (BuildConfig.DEBUG) StrictModeInit.init()

    if (!alreadyCreated) {
      // robolectric creates multiple instances of the Application so we need to prevent
      // additional initializations
      alreadyCreated = true
      Picasso.setSingletonInstance(Picasso.Builder(this).build())
    }

    RxAndroidPlugins.setInitMainThreadSchedulerHandler {
      AndroidSchedulers.from(Looper.getMainLooper(), true)
    }

    CrashlyticsProxy.init(this)
    GlobalScope.launch {
      if (BuildConfig.DEBUG) {
        Timber.plant(Timber.DebugTree())
      } else {
        Timber.plant(CrashLoggingTree())
      }
    }

    component = DaggerAppComponent.builder()
      .application(this)
      .build()
    DataInjector.component = component
    component.inject(this)

    @Suppress("CheckResult")
    themePref.stream
      .distinctUntilChanged()
      .subscribe { theme ->
        AppCompatDelegate.setDefaultNightMode(theme.nightMode)
      }

    bookAdder.scanForFiles()

    autoConnectedReceiver.register(this)

    triggerWidgetOnChange.init()
  }

  companion object {

    lateinit var component: AppComponent
      @VisibleForTesting set
    private var alreadyCreated = false
  }
}
