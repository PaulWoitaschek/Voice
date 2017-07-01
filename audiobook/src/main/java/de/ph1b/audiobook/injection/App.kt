package de.ph1b.audiobook.injection

import android.app.Application
import android.content.Intent
import android.support.v7.app.AppCompatDelegate
import de.ph1b.audiobook.BuildConfig
import de.ph1b.audiobook.features.BookAdder
import de.ph1b.audiobook.features.crashlytics.CrashLoggingTree
import de.ph1b.audiobook.features.crashlytics.CrashlyticsProxy
import de.ph1b.audiobook.misc.value
import de.ph1b.audiobook.persistence.PrefsManager
import de.ph1b.audiobook.playback.PlaybackService
import timber.log.Timber
import javax.inject.Inject


class App : Application() {

  @Inject lateinit var bookAdder: BookAdder
  @Inject lateinit var prefsManager: PrefsManager

  override fun onCreate() {
    super.onCreate()

    CrashlyticsProxy.init(this)

    component = DaggerAppComponent.builder()
        .application(this)
        .build()
    component.inject(this)

    if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
    else Timber.plant(CrashLoggingTree())

    bookAdder.scanForFiles()
    startService(Intent(this, PlaybackService::class.java))

    AppCompatDelegate.setDefaultNightMode(prefsManager.theme.value.nightMode)
  }

  companion object {

    lateinit var component: AppComponent
      private set
  }
}
