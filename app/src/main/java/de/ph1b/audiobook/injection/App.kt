package de.ph1b.audiobook.injection

import android.app.Application
import android.os.Looper
import androidx.appcompat.app.AppCompatDelegate
import de.ph1b.audiobook.BuildConfig
import de.ph1b.audiobook.features.BookAdder
import de.ph1b.audiobook.features.crashlytics.CrashLoggingTree
import de.ph1b.audiobook.features.crashlytics.CrashlyticsProxy
import de.ph1b.audiobook.features.widget.TriggerWidgetOnChange
import de.ph1b.audiobook.koin.AndroidModule
import de.ph1b.audiobook.koin.AppModule
import de.ph1b.audiobook.koin.PersistenceModule
import de.ph1b.audiobook.koin.PlaybackModule
import de.ph1b.audiobook.koin.PrefModule
import de.ph1b.audiobook.misc.StrictModeInit
import de.ph1b.audiobook.persistence.pref.Pref
import de.ph1b.audiobook.playback.AndroidAutoConnectedReceiver
import de.ph1b.audiobook.uitools.ThemeUtil
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.coroutines.experimental.GlobalScope
import kotlinx.coroutines.experimental.launch
import org.koin.android.ext.android.inject
import org.koin.android.ext.android.startKoin
import timber.log.Timber

class App : Application() {

  private val bookAdder: BookAdder by inject()
  private val triggerWidgetOnChange: TriggerWidgetOnChange by inject()
  private val autoConnectedReceiver: AndroidAutoConnectedReceiver by inject()
  private val themePref: Pref<ThemeUtil.Theme> by inject(PrefKeys.THEME)
  private val allowCrashReports: Pref<Boolean> by inject(PrefKeys.CRASH_REPORT_ENABLED)

  override fun onCreate() {
    super.onCreate()

    if (BuildConfig.DEBUG) StrictModeInit.init()

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

    startKoin(this, listOf(PersistenceModule, AppModule, AndroidModule, PrefModule, PlaybackModule))

    CrashlyticsProxy.init(this, allowCrashReports)

    bookAdder.scanForFiles()

    AppCompatDelegate.setDefaultNightMode(themePref.value.nightMode)

    autoConnectedReceiver.register(this)

    triggerWidgetOnChange.init()
  }
}
