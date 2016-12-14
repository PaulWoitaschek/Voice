package de.ph1b.audiobook.injection

import android.app.Application
import android.content.Intent
import android.support.v7.app.AppCompatDelegate
import de.ph1b.audiobook.BuildConfig
import de.ph1b.audiobook.features.BookAdder
import de.ph1b.audiobook.features.firebase.FirebaseCrashReportingTree
import de.ph1b.audiobook.persistence.PrefsManager
import de.ph1b.audiobook.playback.PlaybackService
import timber.log.Timber
import javax.inject.Inject

class App : Application() {

  @Inject lateinit var bookAdder: BookAdder
  @Inject lateinit var prefsManager: PrefsManager

  override fun onCreate() {
    super.onCreate()

    component = newComponent()
    component.inject(this)

    if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
    Timber.plant(FirebaseCrashReportingTree())

    bookAdder.scanForFiles(true)
    startService(Intent(this, PlaybackService::class.java))

    AppCompatDelegate.setDefaultNightMode(prefsManager.theme.get()!!.nightMode)
  }

  private fun newComponent() = DaggerApplicationComponent.builder()
    .androidModule(AndroidModule(this))
    .build()

  companion object {

    lateinit var component: ApplicationComponent
      private set
  }
}