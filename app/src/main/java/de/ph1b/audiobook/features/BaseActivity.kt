package de.ph1b.audiobook.features

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import de.ph1b.audiobook.common.storageMounted
import de.ph1b.audiobook.features.externalStorageMissing.NoExternalStorageActivity
import de.ph1b.audiobook.playback.session.PlaybackService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * Base class for all Activities which checks in onResume, if the storage
 * is mounted. Shuts down service if not.
 */
abstract class BaseActivity : AppCompatActivity() {

  override fun onResume() {
    super.onResume()

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
}
