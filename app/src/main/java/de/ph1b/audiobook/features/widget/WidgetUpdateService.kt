package de.ph1b.audiobook.features.widget

import android.app.Service
import android.content.Intent
import android.content.res.Configuration
import android.os.IBinder
import dagger.android.AndroidInjection
import de.ph1b.audiobook.misc.asV2Observable
import de.ph1b.audiobook.misc.value
import de.ph1b.audiobook.persistence.BookRepository
import de.ph1b.audiobook.persistence.PrefsManager
import de.ph1b.audiobook.playback.PlayStateManager
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import javax.inject.Inject

class WidgetUpdateService : Service() {

  private val disposables = CompositeDisposable()

  @Inject lateinit var prefs: PrefsManager
  @Inject lateinit var repo: BookRepository
  @Inject lateinit var playStateManager: PlayStateManager
  @Inject lateinit var widgetUpdater: WidgetUpdater

  override fun onCreate() {
    AndroidInjection.inject(this)
    super.onCreate()

    // update widget if current book, current book id or playState have changed.
    val anythingChanged = Observable.merge(
        repo.updateObservable().filter { it.id == prefs.currentBookId.value },
        playStateManager.playStateStream(),
        prefs.currentBookId.asV2Observable()
    )
    disposables.add(anythingChanged.subscribe {
      widgetUpdater.update()
    })
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    widgetUpdater.update()
    return Service.START_STICKY
  }

  override fun onDestroy() {
    super.onDestroy()

    disposables.dispose()
  }

  override fun onConfigurationChanged(newCfg: Configuration) {
    val oldOrientation = this.resources.configuration.orientation
    val newOrientation = newCfg.orientation

    if (newOrientation != oldOrientation) {
      widgetUpdater.update()
    }
  }

  override fun onBind(intent: Intent): IBinder? = null
}
