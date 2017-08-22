package de.ph1b.audiobook.injection

import android.app.Activity
import android.app.Application
import android.app.Service
import android.content.Intent
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat.startForegroundService
import android.support.v7.app.AppCompatDelegate
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasActivityInjector
import dagger.android.HasServiceInjector
import dagger.android.support.HasSupportFragmentInjector
import de.ph1b.audiobook.BuildConfig
import de.ph1b.audiobook.features.BookAdder
import de.ph1b.audiobook.features.crashlytics.CrashLoggingTree
import de.ph1b.audiobook.features.crashlytics.CrashlyticsProxy
import de.ph1b.audiobook.misc.value
import de.ph1b.audiobook.persistence.PrefsManager
import de.ph1b.audiobook.playback.PlaybackService
import timber.log.Timber
import javax.inject.Inject

class App : Application(), HasActivityInjector, HasServiceInjector, HasSupportFragmentInjector {

  @Inject lateinit var bookAdder: BookAdder
  @Inject lateinit var prefsManager: PrefsManager
  @Inject lateinit var activityInjector: DispatchingAndroidInjector<Activity>
  @Inject lateinit var serviceInjector: DispatchingAndroidInjector<Service>
  @Inject lateinit var supportFragmentInjector: DispatchingAndroidInjector<Fragment>

  override fun activityInjector() = activityInjector
  override fun serviceInjector() = serviceInjector
  override fun supportFragmentInjector() = supportFragmentInjector

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
    startForegroundService(this, Intent(this, PlaybackService::class.java))

    AppCompatDelegate.setDefaultNightMode(prefsManager.theme.value.nightMode)
  }

  companion object {

    lateinit var component: AppComponent
      private set
  }
}
