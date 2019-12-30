package de.ph1b.audiobook.injection

import android.app.Application
import android.os.Build
import android.os.Looper
import android.webkit.WebView
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatDelegate
import com.squareup.picasso.Picasso
import de.ph1b.audiobook.BuildConfig
import de.ph1b.audiobook.crashreporting.CrashLoggingTree
import de.ph1b.audiobook.crashreporting.CrashReporter
import de.ph1b.audiobook.data.di.DataInjector
import de.ph1b.audiobook.features.BookAdder
import de.ph1b.audiobook.features.widget.TriggerWidgetOnChange
import de.ph1b.audiobook.misc.StrictModeInit
import de.ph1b.audiobook.persistence.pref.Pref
import de.ph1b.audiobook.playback.AndroidAutoConnectedReceiver
import de.ph1b.audiobook.uitools.NightMode
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
  lateinit var themePref: Pref<NightMode>

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

    CrashReporter.init(this)
    GlobalScope.launch {
      if (BuildConfig.DEBUG) {
        Timber.plant(Timber.DebugTree())
      } else {
        Timber.plant(CrashLoggingTree())
      }
    }

    appComponent = AppComponent.builder()
      .application(this)
      .build()
    DataInjector.component = appComponent
    appComponent.inject(this)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      // instantiating a web-view for the first time changes the day night theme.
      // therefore we work around by creating a webview first.
      // https://issuetracker.google.com/issues/37124582
      WebView(this)
    }

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

    private var alreadyCreated = false
  }
}

lateinit var appComponent: AppComponent
  @VisibleForTesting set
