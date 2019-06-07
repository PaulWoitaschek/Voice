package de.ph1b.audiobook.features

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.appcompat.app.AppCompatActivity
import de.ph1b.audiobook.features.externalStorageMissing.NoExternalStorageActivity
import de.ph1b.audiobook.injection.appComponent
import de.ph1b.audiobook.misc.storageMounted
import de.ph1b.audiobook.playback.PlaybackService
import de.ph1b.audiobook.playback.PlayerController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Base class for all Activities which checks in onResume, if the storage
 * is mounted. Shuts down service if not.
 */
abstract class BaseActivity : AppCompatActivity() {

  @Inject
  lateinit var playerController: PlayerController

  init {
    @Suppress("LeakingThis")
    appComponent.inject(this)
  }

  private val connection = object : ServiceConnection {

    override fun onServiceConnected(className: ComponentName, service: IBinder) {
      val binder = service as PlaybackService.PlaybackServiceBinder
      playerController.service = binder.service
    }

    override fun onServiceDisconnected(componentName: ComponentName) {
      playerController.service = null
    }
  }

  override fun onStart() {
    super.onStart()
    bindService(Intent(this, PlaybackService::class.java), connection, Context.BIND_AUTO_CREATE)
  }

  override fun onStop() {
    super.onStop()
    playerController.service = null
    unbindService(connection)
  }

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
