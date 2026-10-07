package voice.core.playback.session

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import java.io.File
import java.util.concurrent.ConcurrentHashMap

@Inject
@SingleIn(AppScope::class)
class ImageFileProvider(private val application: Application) {

  private val uris = ConcurrentHashMap<File, Uri>()

  internal fun uri(file: File): Uri = uris.computeIfAbsent(file) { grantedUri(file) }

  private fun grantedUri(file: File): Uri {
    return FileProvider
      .getUriForFile(
        application,
        application.packageName + ".coverprovider",
        file,
      )
      .also { uri ->
        // These are necessary to grant the cover uri file permissions.
        // systemui is related to this one:
        // https://github.com/PaulWoitaschek/Voice/issues/1860
        //
        // The others are related to watch and car
        // https://github.com/android/uamp/blob/2136c37bcef54da1ee350fd642fc61a744e86654/common/src/main/res/xml/allowed_media_browser_callers.xml
        listOf(
          "com.android.systemui",
          "com.google.android.autosimulator",
          "com.google.android.carassistant",
          "com.google.android.googlequicksearchbox",
          "com.google.android.projection.gearhead",
          "com.google.android.wearable.app",
        ).forEach { grantedPackage ->
          application.grantUriPermission(
            grantedPackage,
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION,
          )
        }
      }
  }
}
