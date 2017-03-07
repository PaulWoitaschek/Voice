package de.ph1b.audiobook.injection

import android.app.Application
import android.content.Intent
import android.support.v7.app.AppCompatDelegate
import com.crashlytics.android.Crashlytics
import com.crashlytics.android.core.CrashlyticsCore
import de.ph1b.audiobook.BuildConfig
import de.ph1b.audiobook.features.BookAdder
import de.ph1b.audiobook.features.firebase.CrashLoggingTree
import de.ph1b.audiobook.misc.value
import de.ph1b.audiobook.persistence.PrefsManager
import de.ph1b.audiobook.playback.PlaybackService
import io.fabric.sdk.android.Fabric
import timber.log.Timber
import javax.inject.Inject


class App : Application() {

  @Inject lateinit var bookAdder: BookAdder
  @Inject lateinit var prefsManager: PrefsManager

  override fun onCreate() {
    super.onCreate()

    val crashlytics = Crashlytics.Builder()
        .core(CrashlyticsCore.Builder()
            .disabled(BuildConfig.DEBUG)
            .build())
        .build()
    Fabric.with(this, crashlytics)

    component = newComponent()
    component.inject(this)

    if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
    else Timber.plant(CrashLoggingTree())

    bookAdder.scanForFiles(true)
    startService(Intent(this, PlaybackService::class.java))

    AppCompatDelegate.setDefaultNightMode(prefsManager.theme.value.nightMode)
  }

  private fun newComponent() = DaggerApplicationComponent.builder()
      .androidModule(AndroidModule(this))
      .build()

  companion object {

    lateinit var component: ApplicationComponent
      private set
  }
}