package de.ph1b.audiobook.features

import android.app.UiModeManager
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.getSystemService
import de.ph1b.audiobook.features.externalStorageMissing.NoExternalStorageActivity
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.injection.PrefKeys
import de.ph1b.audiobook.misc.Observables
import de.ph1b.audiobook.persistence.pref.Pref
import de.ph1b.audiobook.playback.PlaybackService
import de.ph1b.audiobook.uitools.ThemeUtil
import de.ph1b.audiobook.uitools.ThemeUtil.Theme.DAY
import de.ph1b.audiobook.uitools.ThemeUtil.Theme.DAY_NIGHT
import de.ph1b.audiobook.uitools.ThemeUtil.Theme.NIGHT
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.GlobalScope
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext
import org.threeten.bp.LocalTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named

/**
 * Base class for all Activities which checks in onResume, if the storage
 * is mounted. Shuts down service if not.
 */
abstract class BaseActivity : AppCompatActivity() {

  @field:[Inject Named(PrefKeys.THEME)]
  lateinit var themePref: Pref<ThemeUtil.Theme>

  private val onResumeDisposables = CompositeDisposable()
  private lateinit var uiModeManager: UiModeManager

  override fun onCreate(savedInstanceState: Bundle?) {
    App.component.inject(this)
    uiModeManager = getSystemService<UiModeManager>()!!
    val nightMode = themePref.value.nightMode()
    if (uiModeManager.nightMode != nightMode) {
      uiModeManager.nightMode = nightMode
    }
    super.onCreate(savedInstanceState)
  }

  override fun onResume() {
    super.onResume()

    val uiModeManager = getSystemService<UiModeManager>()!!
    val eachSecond = Observable.interval(1, TimeUnit.SECONDS).startWith(0L)
    onResumeDisposables.add(
      Observables
        .combineLatest(themePref.stream, eachSecond) { theme, _ ->
          theme.nightMode()
        }
        .distinctUntilChanged()
        .skip(1)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { nightMode ->
          if (uiModeManager.nightMode != nightMode) {
            uiModeManager.nightMode = nightMode
          }
        }
    )

    GlobalScope.launch(Dispatchers.Main) {
      if (!storageMounted()) {
        val serviceIntent = Intent(this@BaseActivity, PlaybackService::class.java)
        stopService(serviceIntent)

        startActivity(
          Intent(this@BaseActivity, NoExternalStorageActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
          }
        )
        return@launch
      }
    }
  }

  private fun ThemeUtil.Theme.nightMode(): Int {
    val light = when (this) {
      DAY_NIGHT -> {
        val hour = LocalTime.now().hour
        hour in 7..22
      }
      DAY -> true
      NIGHT -> false
    }
    return if (light) {
      UiModeManager.MODE_NIGHT_NO
    } else {
      UiModeManager.MODE_NIGHT_YES
    }
  }

  override fun onPause() {
    super.onPause()
    onResumeDisposables.clear()
  }

  companion object {
    suspend fun storageMounted(): Boolean = withContext(Dispatchers.IO) {
      Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
    }
  }
}
