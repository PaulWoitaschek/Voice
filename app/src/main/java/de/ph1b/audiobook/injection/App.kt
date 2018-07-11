package de.ph1b.audiobook.injection

import android.app.Activity
import android.app.Application
import android.app.Service
import android.content.BroadcastReceiver
import android.support.annotation.VisibleForTesting
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatDelegate
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasActivityInjector
import dagger.android.HasBroadcastReceiverInjector
import dagger.android.HasServiceInjector
import dagger.android.support.HasSupportFragmentInjector
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
import kotlinx.coroutines.experimental.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

class App : Application(), HasActivityInjector, HasServiceInjector, HasSupportFragmentInjector,
  HasBroadcastReceiverInjector {

  @Inject
  lateinit var bookAdder: BookAdder
  @Inject
  lateinit var activityInjector: DispatchingAndroidInjector<Activity>
  @Inject
  lateinit var serviceInjector: DispatchingAndroidInjector<Service>
  @Inject
  lateinit var broadcastInjector: DispatchingAndroidInjector<BroadcastReceiver>
  @Inject
  lateinit var supportFragmentInjector: DispatchingAndroidInjector<Fragment>
  @Inject
  lateinit var triggerWidgetOnChange: TriggerWidgetOnChange
  @Inject
  lateinit var autoConnectedReceiver: AndroidAutoConnectedReceiver
  @field:[Inject Named(PrefKeys.THEME)]
  lateinit var themePref: Pref<ThemeUtil.Theme>

  override fun activityInjector() = activityInjector
  override fun serviceInjector() = serviceInjector
  override fun supportFragmentInjector() = supportFragmentInjector
  override fun broadcastReceiverInjector() = broadcastInjector

  override fun onCreate() {
    super.onCreate()

    if (BuildConfig.DEBUG) StrictModeInit.init()

    launch {
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
    CrashlyticsProxy.init(this, component.allowCrashReports)

    bookAdder.scanForFiles()

    AppCompatDelegate.setDefaultNightMode(themePref.value.nightMode)

    autoConnectedReceiver.register(this)

    triggerWidgetOnChange.init()
  }

  companion object {

    lateinit var component: AppComponent
      @VisibleForTesting set
  }
}
