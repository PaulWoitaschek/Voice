package de.ph1b.audiobook.injection

import android.app.Application
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.color.DynamicColors
import de.paulwoitaschek.flowpref.Pref
import de.ph1b.audiobook.BuildConfig
import de.ph1b.audiobook.common.DARK_THEME_SETTABLE
import de.ph1b.audiobook.common.pref.PrefKeys
import de.ph1b.audiobook.features.widget.TriggerWidgetOnChange
import de.ph1b.audiobook.misc.StrictModeInit
import de.ph1b.audiobook.playback.androidauto.AndroidAutoConnectedReceiver
import de.ph1b.audiobook.playback.di.PlaybackComponent
import de.ph1b.audiobook.playback.di.PlaybackComponentFactoryProvider
import de.ph1b.audiobook.rootComponent
import de.ph1b.audiobook.scanner.MediaScanTrigger
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

class App : Application(), PlaybackComponentFactoryProvider {

  @Inject
  lateinit var mediaScanner: MediaScanTrigger

  @Inject
  lateinit var triggerWidgetOnChange: TriggerWidgetOnChange

  @Inject
  lateinit var autoConnectedReceiver: AndroidAutoConnectedReceiver

  @field:[Inject Named(PrefKeys.DARK_THEME)]
  lateinit var useDarkTheme: Pref<Boolean>

  override fun onCreate() {
    super.onCreate()

    if (BuildConfig.DEBUG) StrictModeInit.init()

    DynamicColors.applyToActivitiesIfAvailable(this)

    if (BuildConfig.DEBUG) {
      Timber.plant(Timber.DebugTree())
    }

    appComponent = AppComponent.factory()
      .create(this)
    rootComponent = appComponent
    appComponent.inject(this)

    if (DARK_THEME_SETTABLE) {
      MainScope().launch {
        useDarkTheme.flow
          .distinctUntilChanged()
          .collect { useDarkTheme ->
            val nightMode = if (useDarkTheme) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            AppCompatDelegate.setDefaultNightMode(nightMode)
          }
      }
    }

    mediaScanner.scan()

    autoConnectedReceiver.register(this)

    triggerWidgetOnChange.init()
  }

  override fun factory(): PlaybackComponent.Factory {
    return appComponent.playbackComponentFactory()
  }
}

lateinit var appComponent: AppComponent
  @VisibleForTesting set
