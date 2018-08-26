package de.ph1b.audiobook.features

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import dagger.android.AndroidInjection
import de.ph1b.audiobook.data.repo.internals.IO
import de.ph1b.audiobook.features.externalStorageMissing.NoExternalStorageActivity
import de.ph1b.audiobook.playback.PlaybackService
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext

/**
 * Base class for all Activities which checks in onResume, if the storage
 * is mounted. Shuts down service if not.
 */
abstract class BaseActivity : AppCompatActivity() {

  private var nightModeAtCreation: Int? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    AndroidInjection.inject(this)
    super.onCreate(savedInstanceState)

    nightModeAtCreation = AppCompatDelegate.getDefaultNightMode()
  }

  override fun onResume() {
    super.onResume()
    launch(UI) {

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

      val nightModesDistinct = AppCompatDelegate.getDefaultNightMode() != nightModeAtCreation
      if (nightModesDistinct) recreate()
    }
  }


  companion object {
    suspend fun storageMounted(): Boolean = withContext(IO) {
      Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
    }
  }
}
