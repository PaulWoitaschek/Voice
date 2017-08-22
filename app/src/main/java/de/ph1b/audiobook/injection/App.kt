package de.ph1b.audiobook.injection

import android.app.Activity
import android.app.Application
import android.app.Service
import android.content.BroadcastReceiver
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatDelegate
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasActivityInjector
import dagger.android.HasBroadcastReceiverInjector
import dagger.android.HasServiceInjector
import dagger.android.support.HasSupportFragmentInjector
import de.ph1b.audiobook.BuildConfig
import de.ph1b.audiobook.features.BookAdder
import de.ph1b.audiobook.features.crashlytics.CrashLoggingTree
import de.ph1b.audiobook.features.crashlytics.CrashlyticsProxy
import de.ph1b.audiobook.features.widget.TriggerWidgetOnChange
import de.ph1b.audiobook.misc.StrictModeInit
import de.ph1b.audiobook.misc.value
import de.ph1b.audiobook.persistence.PrefsManager
import timber.log.Timber
import javax.inject.Inject
import kotlin.concurrent.thread

class App : Application(), HasActivityInjector, HasServiceInjector, HasSupportFragmentInjector, HasBroadcastReceiverInjector {

  @Inject lateinit var bookAdder: BookAdder
  @Inject lateinit var prefsManager: PrefsManager
  @Inject lateinit var activityInjector: DispatchingAndroidInjector<Activity>
  @Inject lateinit var serviceInjector: DispatchingAndroidInjector<Service>
  @Inject lateinit var broadcastInjector: DispatchingAndroidInjector<BroadcastReceiver>
  @Inject lateinit var supportFragmentInjector: DispatchingAndroidInjector<Fragment>
  @Inject lateinit var triggerWidgetOnChange: TriggerWidgetOnChange

  override fun activityInjector() = activityInjector
  override fun serviceInjector() = serviceInjector
  override fun supportFragmentInjector() = supportFragmentInjector
  override fun broadcastReceiverInjector() = broadcastInjector

  override fun onCreate() {
    super.onCreate()

    if (BuildConfig.DEBUG) StrictModeInit.init()

    thread {
      CrashlyticsProxy.init(this)
      if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
      else Timber.plant(CrashLoggingTree())
    }

    component = DaggerAppComponent.builder()
        .application(this)
        .build()
    component.inject(this)

    bookAdder.scanForFiles()

    AppCompatDelegate.setDefaultNightMode(prefsManager.theme.value.nightMode)

    triggerWidgetOnChange.init()
  }

  companion object {

    lateinit var component: AppComponent
      private set
  }
}
