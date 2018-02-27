package de.ph1b.audiobook.features

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.support.v7.app.AppCompatActivity
import android.support.v7.app.AppCompatDelegate
import dagger.android.AndroidInjection
import de.ph1b.audiobook.features.externalStorageMissing.NoExternalStorageActivity
import de.ph1b.audiobook.playback.PlaybackService

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
    if (!storageMounted()) {
      val serviceIntent = Intent(this, PlaybackService::class.java)
      stopService(serviceIntent)

      startActivity(
        Intent(this, NoExternalStorageActivity::class.java).apply {
          flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
      )
      return
    }

    val nightModesDistinct = AppCompatDelegate.getDefaultNightMode() != nightModeAtCreation
    if (nightModesDistinct) recreate()
  }

  companion object {
    fun storageMounted(): Boolean {
      return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
    }
  }
}
