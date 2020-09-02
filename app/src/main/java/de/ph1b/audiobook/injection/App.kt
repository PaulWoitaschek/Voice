package de.ph1b.audiobook.injection

import android.app.Application
import android.os.Build
import android.webkit.WebView
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatDelegate
import com.squareup.picasso.Picasso
import de.paulwoitaschek.flowpref.Pref
import de.ph1b.audiobook.BuildConfig
import de.ph1b.audiobook.common.pref.PrefKeys
import de.ph1b.audiobook.features.widget.TriggerWidgetOnChange
import de.ph1b.audiobook.misc.DARK_THEME_SETTABLE
import de.ph1b.audiobook.misc.StrictModeInit
import de.ph1b.audiobook.playback.androidauto.AndroidAutoConnectedReceiver
import de.ph1b.audiobook.playback.di.PlaybackComponent
import de.ph1b.audiobook.playback.di.PlaybackComponentFactoryProvider
import de.ph1b.audiobook.scanner.MediaScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

class App : Application(), PlaybackComponentFactoryProvider {

  @Inject
  lateinit var mediaScanner: MediaScanner

  @Inject
  lateinit var triggerWidgetOnChange: TriggerWidgetOnChange

  @Inject
  lateinit var autoConnectedReceiver: AndroidAutoConnectedReceiver

  @field:[Inject Named(PrefKeys.DARK_THEME)]
  lateinit var useDarkTheme: Pref<Boolean>

  override fun onCreate() {
    super.onCreate()

    if (BuildConfig.DEBUG) StrictModeInit.init()

    if (!alreadyCreated) {
      // robolectric creates multiple instances of the Application so we need to prevent
      // additional initializations
      alreadyCreated = true
      Picasso.setSingletonInstance(Picasso.Builder(this).build())
    }

    if (BuildConfig.DEBUG) {
      Timber.plant(Timber.DebugTree())
    }

    appComponent = AppComponent.factory()
      .create(this)
    appComponent.inject(this)

    if (DARK_THEME_SETTABLE && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      // instantiating a web-view for the first time changes the day night theme.
      // therefore we work around by creating a webview first.
      // https://issuetracker.google.com/issues/37124582
      WebView(this)
    }

    if (DARK_THEME_SETTABLE) {
      @Suppress("CheckResult")
      GlobalScope.launch(Dispatchers.Main) {
        useDarkTheme.flow
          .distinctUntilChanged()
          .collect { useDarkTheme ->
            val nightMode = if (useDarkTheme) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            AppCompatDelegate.setDefaultNightMode(nightMode)
          }
      }
    }

    mediaScanner.scanForFiles()

    autoConnectedReceiver.register(this)

    triggerWidgetOnChange.init()
  }

  override fun factory(): PlaybackComponent.Factory {
    return appComponent.playbackComponentFactory()
  }

  companion object {

    private var alreadyCreated = false
  }
}

lateinit var appComponent: AppComponent
  @VisibleForTesting set
