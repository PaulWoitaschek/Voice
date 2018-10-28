package de.ph1b.audiobook.injection

import android.app.Application
import android.app.UiModeManager
import android.os.Looper
import androidx.annotation.VisibleForTesting
import androidx.core.content.getSystemService
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
import kotlinx.coroutines.experimental.GlobalScope
import kotlinx.coroutines.experimental.launch
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
    CrashlyticsProxy.init(this)

    val uiModeManager = getSystemService<UiModeManager>()!!
    @Suppress("CheckResult")
    themePref.stream
      .distinctUntilChanged()
      .subscribe {
        uiModeManager.nightMode = it.nightMode
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
